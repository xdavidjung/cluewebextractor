cluewebextractor
================
cluewebextractor takes warc files from Clueweb and extracts sentences from 
each warc record's payload.

On error, cluewebextractor prefers to skip over the smallest possible amount of 
data rather than crash. The amount skipped can be either a sentence, a warc 
record, or an entire warc file (which should be relatively rare).

For each sentence found, outputs a tab-separated line with fields: 
warc trec-id, url, sentence number, sentence

Quick Start:
------------

To make the jar file, run:

mvn compile scala:compile assembly:single

Usage:
------

java -jar \<jarfile\> \<input\> --output-dir \<output-dir\>

Inputs can be either a single file or a directory. If a directory, 
cluewebextractor will find all the .warc and .gz files inside of that 
directory and extract content from them.

--output-dir is an optional switch that specifies an output directory for the 
extracted content. If not used, cluewebextractor will either: 
not use a directory, if input is a single file, or; use the name of the 
input directory as the output directory, if input is a directory.

The extractor will print out to an output file for each input file. 