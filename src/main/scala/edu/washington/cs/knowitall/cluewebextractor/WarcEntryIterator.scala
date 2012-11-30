package edu.washington.cs.knowitall.cluewebextractor

// iterator over war entries in a warc file
class WarcEntryIterator(file: Iterator[String]) extends Iterator[WarcEntry] {
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
    if (current.equals(WarcEntryIterator.NewWarcEntryIndicator)) return true;

    // a warc file has a next warcEntry if we can find a line in fileIt that
    // is equal to "WARC/0.18"
    while (fileIt.hasNext) {
      nextLine();
      if (current.equals(WarcEntryIterator.NewWarcEntryIndicator)) return true;
    }

    // hasNext returned false: no more warc entries, this iterator is no longer valid
    valid = false;
    return false;
  }

  private def nextLine(): String = {
    if (!fileIt.hasNext) throw new NoSuchElementException();
    current = fileIt.next();
    println(current);
    return current;
  }

  // returns the next WarcEntry in this file.
  // returns null for the header.
  // throws NoSuchElementException if there is no next WarcEntry.
  def next(): WarcEntry = {
    if (!hasNext) throw new NoSuchElementException();

    // start constructing the warc entry fields
    val wType = nextLine().split(": ")(1);
    if (wType.equals(WarcEntryIterator.HeaderIndicator)) return null; // header

    val wTargetUri = nextLine().split(": ", -1)(1);
    val wWarcinfoId = nextLine().split(": ", -1)(1);
    val wDate = nextLine().split(": ", -1)(1);
    val wRecordId = nextLine().split(": ", -1)(1);
    val wTrecId = nextLine().split(": ", -1)(1);
    val contentType = nextLine().split(": ", -1)(1);
    val wIdentifiedPayloadType = nextLine().split(": ", -1)(1);
    val contentLength = nextLine().split(": ", -1)(1).toInt;

    // now get the payload
    val sb = new StringBuilder(contentLength);
    // grab the newline between the header and the payload
    nextLine();
    while (fileIt.hasNext) {
      nextLine();
      if ((current.equals(WarcEntryIterator.NewWarcEntryIndicator)) ||
          (sb.capacity < sb.length + current.length)) {
        return new WarcEntry(
          wType,
          wTargetUri,
          wWarcinfoId,
          wDate,
          wRecordId,
          wTrecId,
          contentType,
          wIdentifiedPayloadType,
          contentLength,
          sb.toString);
      }
      sb.append(current)
    }
    return new WarcEntry(
      wType,
      wTargetUri,
      wWarcinfoId,
      wDate,
      wRecordId,
      wTrecId,
      contentType,
      wIdentifiedPayloadType,
      contentLength,
      sb.toString);
  }
}

object WarcEntryIterator {
  val NewWarcEntryIndicator = "WARC/0.18";
  val HeaderIndicator = "warcinfo";
}
