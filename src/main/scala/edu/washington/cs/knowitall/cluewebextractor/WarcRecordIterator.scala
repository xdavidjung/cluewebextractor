package edu.washington.cs.knowitall.cluewebextractor

// iterator over war entries in a warc file
class WarcRecordIterator(file: Iterator[String]) extends Iterator[WarcRecord] {
  // iterator over the file that this warc entry iterator is looking at
  val fileIt: Iterator[String] = file;

  // the latest line that fileIt has returned
  var current: String = "";

  // whether this iterator is valid
  var valid = true;

  // returns true if there is another warc entry in file.
  // mutates the state of file: if hasNext returns true, then fileIt will be at
  // the beginning of the next warcEntry
  def hasNext(): Boolean = {
    if (!valid) return false;

    // check if fileIt is already at the next entry
    if (current.equals(WarcRecordIterator.NewWarcRecordIndicator)) return true;

    // a warc file has a next warcEntry if we can find a line in fileIt that
    // is equal to "WARC/0.18"
    while (fileIt.hasNext) {
      nextLine();
      if (current.equals(WarcRecordIterator.NewWarcRecordIndicator)) return true;
    }

    // hasNext returned false: no more warc entries, this iterator is no longer valid
    valid = false;
    return false;
  }

  private def nextLine(): String = {
    if (!fileIt.hasNext) throw new NoSuchElementException();
    current = fileIt.next();
    return current;
  }

  // data has been observed to have arbitrary carriage returns: this matters
  // for the WARC header fields.
  // this method will handle this bad data by checking to see if the next
  // line (which should be a header field) is in the proper
  //    "[header key]: [header value]"
  // format.
  // if not, appends whatever is there to the previous field until it finds
  // a line that's properly formatted and returns it.
  // mutates previous field, the line iterator, and current.
  def getNextHeaderField(previousField: StringBuilder): StringBuilder = {
    var nextHeader = nextLine().split(": ")
    while (nextHeader.length == 1) {
      if (previousField == null)
        throw new IllegalArgumentException("getNextHeaderField: bad header")

      previousField.append(nextHeader(0))
      nextHeader = nextLine().split(": ")
    }
    return new StringBuilder(nextHeader(1))
  }

  // returns the next WarcRecord in this file.
  // returns null for the header.
  // throws NoSuchElementException if there is no next WarcRecord.
  def next(): WarcRecord = {
    if (!hasNext) throw new NoSuchElementException();

    // start constructing the warc entry fields
    val wType = getNextHeaderField(null);
    if (wType.toString.equals(WarcRecordIterator.HeaderIndicator)) return null  // header

    val wTargetUri = getNextHeaderField(wType);
    val wWarcinfoId = getNextHeaderField(wTargetUri)
    val wDate = getNextHeaderField(wWarcinfoId)
    val wRecordId = getNextHeaderField(wDate)
    val wTrecId = getNextHeaderField(wRecordId)
    val contentType = getNextHeaderField(wTrecId)
    nextLine()  // skip over the identified payload type
    val contentLength =
      getNextHeaderField(null).toString.toInt;

    // now get the payload
    val sb = new StringBuilder(contentLength);
    nextLine();  // grab the line between WARC header and HTTP header
    do {  // grab lines until the line between HTTP header and content
      nextLine();
    } while (current.length > 0)

    while (fileIt.hasNext) {
      nextLine();
      if ((current.equals(WarcRecordIterator.NewWarcRecordIndicator))) {
        return new WarcRecord(
          wType.toString,
          wTargetUri.toString,
          wWarcinfoId.toString,
          wDate.toString,
          wRecordId.toString,
          wTrecId.toString,
          contentType.toString,
          contentLength,
          sb.toString);
      }
      sb.append(current)
      sb.append(" ")  // append some whitespace to represent a line break
    }
    // at end of the file: return what we have
    return new WarcRecord(
      wType.toString,
      wTargetUri.toString,
      wWarcinfoId.toString,
      wDate.toString,
      wRecordId.toString,
      wTrecId.toString,
      contentType.toString,
      contentLength,
      sb.toString);
  }
}

object WarcRecordIterator {
  val NewWarcRecordIndicator = "WARC/1.0";
  val HeaderIndicator = "warcinfo";
}
