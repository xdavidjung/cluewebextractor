package edu.washington.cs.knowitall.cluewebextractor

/**
 * Represents the warcinfo record that acts as a header for warc files.
 */
case class WarcinfoHeader(
  WARC-Type: String,
  WARC-Date: String,
  WARC-Record-ID: String,
  Content-Length: Int,
  WARC-Number-Of-Documents: int,
  WARC-File-Length: int,
  WARC-Data-Type: String,
  payload: String
);
