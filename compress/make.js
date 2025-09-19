import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {join} from 'node:path';
import {readFileSync, writeFileSync} from 'node:fs';
import assert from 'node:assert';
import {Encoder} from './Encoder.js';
import {Magic} from './Magic.js';
import {
	compare_arrays, transpose, group_by, collect_while, same, 
	read_unique, read_unsorted_deltas, read_sorted_ascending, read_tree, read_str
} from './utils.js';

const BASE_DIR = fileURLToPath(new URL('.', import.meta.url));
const DATA_DIR = join(BASE_DIR, 'data');
const RESOURCES_DIR = join(BASE_DIR, '../lib/src/main/resources/');

const spec_bytes = readFileSync(join(DATA_DIR, 'spec.json'));
const SPEC = JSON.parse(spec_bytes);
const NF = JSON.parse(readFileSync(join(DATA_DIR, 'nf.json')));

{
	const {created, unicode, cldr} = SPEC;
	console.log({
		created, unicode, cldr,
		spec_hash: createHash('sha256').update(spec_bytes).digest().toString('hex'),
	});
}

let decomp = NF.decomp.map(x => x.flat()).sort(compare_arrays);
let decomp1 = decomp.filter(x => x.length === 2);
let decomp2 = decomp.filter(x => x.length === 3);
let mapped = SPEC.mapped.map(x => x.flat()).sort(compare_arrays);

function bit_flags_from_group(g) {
	return (g.restricted?1:0) + (g.cm?2:0);
}

const enc_nf = new Encoder();
enc_nf.str(NF.unicode.split(' ')[0]);
enc_nf.unique(NF.exclusions);
enc_nf.unique(NF.qc);

enc_nf.unique(decomp1.map(x => x[0]));
enc_nf.unsorted_deltas(decomp1.map(x => x[1]));
enc_nf.unique(decomp2.map(x => x[0]));
enc_nf.unsorted_deltas(decomp2.map(x => x[1]));
enc_nf.unsorted_deltas(decomp2.map(x => x[2]));

for (let v of NF.ranks) {
	enc_nf.unique(v);
}
enc_nf.unique([]);

const enc_spec = new Encoder();
enc_spec.unique(SPEC.escape);
enc_spec.unique(SPEC.ignored);
enc_spec.unique(SPEC.cm);
enc_spec.symbol(SPEC.nsm_max);
enc_spec.unique(SPEC.nsm);
enc_spec.unique(SPEC.nfc_check);

enc_spec.symbol(SPEC.fenced.length)
enc_spec.sorted_ascending(SPEC.fenced.map(x => x[0]));
SPEC.fenced.forEach(x => enc_spec.str(x[1]));

for (let v of group_by(SPEC.mapped, x => x[1].length)) {
	let m = transpose(v.map(x => x[1]));
	enc_spec.symbol(m.length);
	enc_spec.unique(v.map(x => x[0]));
	for (let u of m) {
		enc_spec.unsorted_deltas(u);
	}
}
enc_spec.symbol(0);

for (let g of SPEC.groups) {
	enc_spec.str(g.name);
	enc_spec.symbol(bit_flags_from_group(g));
	enc_spec.unique(g.primary);
	enc_spec.unique(g.secondary);	
}
enc_spec.str('');

enc_spec.tree(SPEC.emoji);
for (let x of SPEC.wholes) {
	enc_spec.unique(x.confused);
	enc_spec.unique(x.valid);
}
enc_spec.unique([]);

let magic_nf   = new Magic([2,6,8,11,14,15,18]);
let magic_spec = new Magic([1,3,7,13,16,17,18,19]);
let bytes_nf   = magic_nf.bytes_from_symbols(enc_nf.symbols);
let bytes_spec = magic_spec.bytes_from_symbols(enc_spec.symbols);

if (0) { // enable to recompute (very slow)
	[magic_nf, bytes_nf] = Magic.optimize(enc_nf.symbols, 20); 
	[magic_spec, bytes_spec] = Magic.optimize(enc_spec.symbols, 20);
}

console.log(`  NF: ${bytes_nf.length} using ${magic_nf.widths}`);
console.log(`Spec: ${bytes_spec.length} using ${magic_spec.widths}`);

function align_buf(buf, align) {
	let x = buf.length % align;
	if (x) buf = Buffer.concat([buf, Buffer.alloc(align - x)]);
	return buf;
}

writeFileSync(join(RESOURCES_DIR, 'nf.bin'),   align_buf(bytes_nf, 4));
writeFileSync(join(RESOURCES_DIR, 'spec.bin'), align_buf(bytes_spec, 4));

const r1 = Magic.reader_from_bytes(bytes_nf);
console.log(read_str(r1));
assert(same(NF.exclusions, read_unique(r1)));
assert(same(NF.qc, read_unique(r1)));

let decomp1A = read_unique(r1);
let decomp1B = read_unsorted_deltas(decomp1A.length, r1);
let decomp2A = read_unique(r1);
let decomp2B = read_unsorted_deltas(decomp2A.length, r1);
let decomp2C = read_unsorted_deltas(decomp2A.length, r1);
assert(same(decomp, [
	...decomp1A.map((x, i) => [x, decomp1B[i]]),
	...decomp2A.map((x, i) => [x, decomp2B[i], decomp2C[i]]),
].sort(compare_arrays)))

assert(same(NF.ranks, collect_while(() => {
	let v = read_unique(r1);
	if (v.length) return v;
})));

const r2 = Magic.reader_from_bytes(bytes_spec);
assert(same(SPEC.escape, read_unique(r2)));
assert(same(SPEC.ignored, read_unique(r2)));
assert(same(SPEC.cm, read_unique(r2)));
assert(same(SPEC.nsm_max, r2()));
assert(same(SPEC.nsm, read_unique(r2)));
assert(same(SPEC.nfc_check, read_unique(r2)));
assert(same(SPEC.fenced, read_sorted_ascending(r2(), r2).map(x => [x, read_str(r2)])));

assert(same(mapped, collect_while(() => {
	let w = r2();
	if (w) {
		let m = read_unique(r2).map(x => [x]);
		for (let i = 0; i < w; i++) {
			read_unsorted_deltas(m.length, r2).forEach((x, i) => m[i].push(x));
		}
		return m;
	}
}).flat().sort(compare_arrays)));

assert(same(SPEC.groups.map(x => [
	x.name,
	bit_flags_from_group(x),
	x.primary,
	x.secondary
]), collect_while(() => {
	let name = read_str(r2);
	if (name) {
		return [name, r2(), read_unique(r2), read_unique(r2)];
	}
})));

assert(same(SPEC.emoji, read_tree(r2)));
assert(same(SPEC.wholes.map(x => [x.confused, x.valid]), collect_while(() => {
	let confused = read_unique(r2);
	if (confused.length) {
		let valid = read_unique(r2);
		return [confused, valid];
	}
})));
