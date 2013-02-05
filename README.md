cluewebextractor
================
cluewebextractor takes warc files from Clueweb and extracts sentences from them.

For each sentence found, outputs a tab-separated line with fields: warc id, uri, sentence number, sentence.

Note that in order to run the user must supply a directory of language profiles, which are needed by the language detection module (http://code.google.com/p/language-detection/). The default directory to use is in the base directory of this repo, but will also come packaged when the code is compiled, in target/classes/edu/washington/cs/knowitall/cluewebextractor/profiles.

To make the jar file, run:

mvn compile scala:compile assembly:single

java -jar \<jarfile\> \<warcfile\> \<profiles-directory\>

Prints to standard output.
