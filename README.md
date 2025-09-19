# ENSNormalize.java
0-dependency [ENSIP-15](https://docs.ens.domains/ensip/15) in Java

* Reference Implementation: [adraffy/ens-normalize.js](https://github.com/adraffy/ens-normalize.js)
	* Unicode: `17.0.0`
	* Spec Hash: [`4febc8f5d285cbf80d2320fb0c1777ac25e378eb72910c34ec963d0a4e319c84`](https://github.com/adraffy/ens-normalize.js/blob/main/derive/output/spec.json)
* Passes **100%** [ENSIP-15 Validation Tests](https://github.com/adraffy/ens-normalize.js/blob/main/validate/tests.json)
* Passes **100%** [Unicode Normalization Tests](https://github.com/adraffy/ens-normalize.js/blob/main/derive/output/nf-tests.json)
* Space Efficient: `~58KB .jar` using [binary resources](./lib/src/main/resources/) via [make.js](./compress/make.js)
* JDK Support: `8+`
* Maven Central Repository: [`io.github.adraffy`
](https://central.sonatype.com/artifact/io.github.adraffy/ens-normalize/) — `0.3.0`
```java
import io.github.adraffy.ens.ENSNormalize;
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

### Additional [NormDetails](./lib/src/main/java/io/github/adraffy/ens/NormDetails.java) (Experimental)
```java
// works like normalize(), throws on invalid names
// string -> NormDetails
NormDetails details = ENSNormalize.ENSIP15.normalizeDetails("💩ì.a");

String name; // normalized name
boolean possiblyConfusing; // if name should be carefully reviewed
HashSet<Group> groups; // unique groups in name
HashSet<EmojiSequence> emojis; // unique emoji in name
String groupDescription() = "Emoji+Latin"; // group summary for name
boolean hasZWJEmoji(); // if any emoji contain 200D
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
import io.github.adraffy.ens.ENSNormalize;

// String -> String
ENSNormalize.NF.NFC("\u0065\u0300"); // "\u00E8"
ENSNormalize.NF.NFD("\u00E8");       // "\u0065\u0300"

// int[] -> int[]
ENSNormalize.NF.NFC(0x65, 0x300); // [0xE8]
ENSNormalize.NF.NFD(0xE8);        // [0x65, 0x300]
```

## Publish Instructions

* [Sync and Compress](./compress/)
* Update [Gradle](https://gradle.org/install/): `./gradlew wrapper --gradle-version {VERSION}`
* Run Tests: `./gradlew test`
* Ensure [Access Token](https://central.sonatype.com/)
* Publish and Sign: `./gradlew publish`
* [Close and Release](https://central.sonatype.com/publishing)
