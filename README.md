# ENSNormalize.java
0-dependency [ENSIP-15](https://docs.ens.domains/ens-improvement-proposals/ensip-15-normalization-standard) in Java

* Reference Implementation: [@adraffy/ens-normalize.js](https://github.com/adraffy/ens-normalize.js)
	* Unicode: `15.0.0`
	* Spec Hash: [`962316964553fce6188e25a5166a4c1e906333adf53bdf2964c71dedc0f8e2c8`](https://github.com/ensdomains/docs/blob/master/ens-improvement-proposals/ensip-15/spec.json)
* Passes **100%** [ENSIP-15 Validation Tests](https://github.com/ensdomains/docs/blob/master/ens-improvement-proposals/ensip-15/tests.json)
* Passes **100%** [Unicode Normalization Tests](https://unicode.org/Public/15.0.0/ucd/NormalizationTest.txt)
* Space Efficient: `~61KB .jar` using [binary resources](./lib/src/main/resources/) via [make.js](./compress/make.js)
* JDK Support: `8+`

```java
import io.github.adraffy.ens;
ENSNormalize.ENSIP15 // Main Library (global instance)
```

### Primary API [ENSIP15](./lib/src/main/java/io/github/adraffy/ens/ENSIP15.java)

```java
// String -> String
// throws on invalid names
ENSNormalize.ENSIP15.normalize("RaFFY🚴‍♂️.eTh"); // "raffy🚴‍♂.eth"

// works like normalize()
ENSNormalize.ENSIP15.beautify("1⃣2⃣.eth"); // "1️⃣2️⃣.eth"
```

### Output-based Tokenization [Label](./lib/src/main/java/io/github/adraffy/ens/Label.java)

```java
// String -> List<Label>
// never throws
List<Label> labels = ENSNormalize.ENSIP15.split("💩Raffy.eth_");
// [
//   Label {
//     input: [ 128169, 82, 97, 102, 102, 121 ],  
//     tokens: [
//       OutputToken { cps: [ 128169 ], emoji: EmojiSequence { ... } }
//       OutputToken { cps: [ 114, 97, 102, 102, 121 ] }
//     ],
//     normalized: [ 128169, 114, 97, 102, 102, 121 ],
//     group: Group { name: "Latin", ... }
//   },
//   Label {
//     input: [ 101, 116, 104, 95 ],
//     tokens: [ 
//       OutputToken { cps: [ 101, 116, 104, 95 ] }
//     ],
//     error: NormException { kind: "underscore allowed only at start" }
//   }
// ]
```

### Normalization Properties

* [Group](./lib/src/main/java/io/github/adraffy/ens/Group.java) — `ENSIP15.groups: List<Group>`
* [EmojiSequence](./lib/src/main/java/io/github/adraffy/ens/EmojiSequence.java) — `ENSIP15.emojis: List<EmojiSequence>`
* [Whole](./lib/src/main/java/io/github/adraffy/ens/Whole.java) — `ENSIP15.wholes: List<Whole>`

### Error Handling

All errors are safe to print. [NormException](./lib/src/main/java/io/github/adraffy/ens/NormException.java) `{ kind: string, reason: string? }` is the base exception.  Functions that accept names as input wrap their exceptions in [InvalidLabelException](./lib/src/main/java/io/github/adraffy/ens/InvalidLabelException.java) `{ start, end, error: NormException }` for additional context.

* `"disallowed character"` — [DisallowedCharacterException](./lib/src/main/java/io/github/adraffy/ens/DisallowedCharacterException.java) `{ cp }`
* `"illegal mixture"` — [IllegalMixtureException](./lib/src/main/java/io/github/adraffy/ens/IllegalMixtureException.java) `{ cp, group, other? }`
* `"whole-script confusable"` — [ConfusableException](./lib/src/main/java/io/github/adraffy/ens/ConfusableException.java) `{ group, other }`
* `"empty label"`
* `"duplicate non-spacing marks"`
* `"excessive non-spacing marks"`
* `"leading fenced"`
* `"adjacent fenced"`
* `"trailing fenced"`
* `"leading combining mark"`
* `"emoji + combining mark"`
* `"invalid label extension"`
* `"underscore allowed only at start"`

### Utilities

Normalize name fragments for substring search:
```java
// String -> String
// only throws InvalidLabelException w/DisallowedCharacterException
ENSNormalize.ENSIP15.normalizeFragment("AB--");
ENSNormalize.ENSIP15.normalizeFragment("..\u0300");
ENSNormalize.ENSIP15.normalizeFragment("\u03BF\u043E");
// note: normalize() throws on these
```

Construct safe strings:
```java
// int -> String
ENSNormalize.ENSIP15.safeCodepoint(0x303); // "◌̃ {303}"
ENSNormalize.ENSIP15.safeCodepoint(0xFE0F); // "{FE0F}"
// int[] -> String
ENSNormalize.ENSIP15.safeImplode(0x303, 0xFE0F); // "◌̃{FE0F}"
```
Determine if a character shouldn't be printed directly:
```java
// ReadOnlyIntSet 
ENSNormalize.ENSIP15.shouldEscape.contains(0x202E); // RIGHT-TO-LEFT OVERRIDE => true
```
Determine if a character is a combining mark:
```java
// ReadOnlyIntSet
ENSNormalize.ENSIP15.combiningMarks.contains(0x20E3); // COMBINING ENCLOSING KEYCAP => true
```

### Unicode Normalization Forms [NF](./lib/src/main/java/io/github/adraffy/ens/NF.java)

```java
import io.github.adraffy.ens;

// String -> String
ENSNormalize.NF.NFC("\u0065\u0300"); // "\u00E8"
ENSNormalize.NF.NFD("\u00E8");       // "\u0065\u0300"

// int[] -> int[]
ENSNormalize.NF.NFC(0x65, 0x300); // [0xE8]
ENSNormalize.NF.NFD(0xE8);        // [0x65, 0x300]
```
