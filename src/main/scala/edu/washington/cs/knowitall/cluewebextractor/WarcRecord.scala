package edu.washington.cs.knowitall.cluewebextractor

/**
 * This class represents a WARC Record conforming to WARC ISO 28500 v. 1.0,
 * described here:
 *   http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
 * WARC Records consist of a record header followed by a record content block
 * and two newlines.
 *
 * The record header begins with a line declaring the record to be in the WARC
 * format, the version (in this case 1.0), followed by a variable number of
 * named fields terminated by a blank line.
 * Named fields may appear in any order, but all follow the general form:
 * [field-name]": "[field-value]
 * The following fields are mandatory:
 *   WARC-Record-ID
 *        A legal URI that is "globally unique for its
 *        period of intended use"
 *   Content-Length
 *        The number of octets in the block.
 *   WARC-Date
 *        A 14-digit UTC timestamp formatted according to
 *        YYYY-MM-DDThh:mm:ssZ.
 *   WARC-Type
 *        The type of record: one of "warcinfo", "response",
 *        "resource", "request", "metadata", "revisit",
 *        "conversion", or "continuation".
 *        More information on each of these types is below.
 *
 * The following fields are not mandatory:
 *   Content-Type
 *        The MIME type of the information in the record's block.
 *   WARC-Concurrent-To
 *        The WARC-Record-IDs of any records create as part of
 *        the same "capture event" as the current record.
 *        This field associates records of types "request",
 *        "response", "resource", "metadata", and "revisit"
 *        with one another when they arise from a single
 *        capture event.
 *        NOTE: This field may repeat within the same record.
 *   WARC-Block-Digest
 *        An optional parameter indicating the algorithm name
 *        and calculated value of a digest applied to the full
 *        block of the record.
 *   WARC-Payload-Digest 
 *        An optional parameter indicating the algorithm name
 *        and calculated value of a digest applied to the
 *        payload referred to or contained by the record: not
 *        necessarily the same as the record block.
 *   WARC-IP-Address
 *        The numeric Internet address contacted to retrieve
 *        any included content. May be either IPv4/6.
 *   WARC-Refers-To
 *        The WARC-Record-ID of a single record for which
 *        the present record holds additional content.
 *        Used to associate a 'metadata' record to another
 *        record that it describes, or 'revisit', 'conversion'.
 *   WARC-Target-URI
 *        The original URI whose capture gave rise to the
 *        information content in this record.
 *   WARC-Truncated
 *        If this field is present, the current record was
 *        truncated, and the reason is given as the field value.
 *        Can be one of length, time, disconnect, unspecified.
 *        A truncated record will have an accurate Content-
 *        Length field.
 *   WARC-Warcinfo-ID
 *        Indicates the Warc-Record-ID of the associated
 *        warcinfo record for this record. Typically used when
 *        the context of the associated warcinfo record is
 *        unavailable. The presence and value of this record
 *        overwrites any association with a previously occurring
 *        warcinfo record in the same file.
 *   WARC-Filename
 *        The filename containing the current 'warcinfo' record.
 *        Only used for 'warcinfo'-type records.
 *   WARC-Profile
 *        URI signifying the kind of analysis and handling
 *        applied in a 'revisit' record.
 *   WARC-Identified-Payload-Type
 *        The content-type of the record's payload as determined
 *        by an independent check. Only used for records with a
 *        well-defined payload.
 *   WARC-Segment-Number
 *        Reports the current record's relative ordering in a
 *        sequence of segmented records. This is mandatory in
 *        any record that is completed in one or more later
 *        'continuation' WARC records. 
 *   WARC-Segment-Origin-ID
 *        Identifies the starting record in a series of
 *        segmented records.
 *   WARC-Segment-Total-Length
 *        Reports the total length of all segment content blocks
 *        when concatenated together. Only used in the last
 *        record in a series of segmented records.
 * 
 * There are some fields unique to the ClueWeb12 dataset:
 * For warcinfo records only:
 *   WARC-Number-Of-Documents
 *        The number of ClueWeb12 documents contained in the file.
 *   WARC-File-Length
 *        The length, in bytes, of the uncompressed file
 *   WARC-Data-Type
 *        A short description of the type of documents contained
 *        in the file, i.e. web crawl.
 * For warc records:
 *   WARC-TREC-ID
 *        A globally unique identifier for the dataset that
 *        describes the location of the individual record within
 *        the entire ClueWeb12 dataset. In the format:
 *        clueweb12-<directory>-<file>-<record>
 */

case class WarcRecord(
  warcType: String,
  warcTrecId: String,
  payload: String
);
