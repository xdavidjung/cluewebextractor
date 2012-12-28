cluewebextractor
================
cluewebextractor takes warc files from Clueweb and extracts sentences from them.

For each sentence found, outputs a tab-separated line with fields: warc id, uri, sentence number, sentence.

To make the jar file, run:

mvn compile scala:compile assembly:single

java -jar \<jarfile\> \<warcfile\>

Prints to standard output.
