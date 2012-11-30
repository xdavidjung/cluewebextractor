package edu.washington.cs.knowitall.cluewebextractor

case class WarcEntry(
  warcType: String,
  warcTargetUri: String,
  warcWarcinfoId: String,
  warcDate: String,
  warcRecordId: String,
  warcTrecId: String,
  contentType: String,
  warcIdentifiedPayloadType: String,
  contentLength: Int,
  payload: String
);