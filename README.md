cluewebextractor
================
cluewebextractor takes warc files from Clueweb and extracts sentences from them.

For each sentence found, outputs a tab-separated line with fields: warc id, sentence number, sentence.

Note that in order to run the user must supply a directory of language profiles, which are needed by the language detection module (http://code.google.com/p/language-detection/). The default directory to use is in the base directory of this repo.

To make the jar file, run:

mvn compile scala:compile assembly:single

There are two ways to supply input: either a .warc file or by passing in the contents of a warc file through standard in, such as piping gunzip -c output.

Usage when supplying a .warc file:

java -jar \<jarfile\> \<profiles-directory\> \<warcfile\>

Usage when supplying content through standard in:

java -jar \<jarfile\> \<profiles-directory\>

Prints to standard output.
