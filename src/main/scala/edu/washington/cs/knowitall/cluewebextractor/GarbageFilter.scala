package edu.washington.cs.knowitall.cluewebextractor

// this class contains a number of methods that detect garbage in sentences
// taken from the web.
class GarbageFilter {

  // runs all of the different filtering methods on a single input:
  // returns true if input contains garbage
  def containsGarbage(input: String): Boolean = {
    return false;
  }

  // returns true if input contains raw html tags
  def containsHtmlTags(input: String): Boolean = {
    return false;
  }

  // returns true if input contains less than three words (detected by the
  // number of spaces in input)
  def tooShort(input: String): Boolean = {
    return false;
  }
}
