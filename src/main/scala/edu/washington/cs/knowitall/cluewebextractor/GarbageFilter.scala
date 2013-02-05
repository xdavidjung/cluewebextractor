package edu.washington.cs.knowitall.cluewebextractor
import org.jsoup.Jsoup;
import scala.util.matching.Regex;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

// this class contains a number of methods that detect garbage in sentences
// taken from the web.
class GarbageFilter(dir: String) {

  // initialize the DetectorFactory with the default profile
  DetectorFactory.loadProfile(dir);

  // removes series of extraneous whitespace in the input sentence
  def removeWhitespace(input: String): String = {
    new Regex("""\s+""").replaceAllIn(input, " ")
  }

  // returns whether all the characters in input are within an acceptable
  // range of characters (common english chars).
  def onlyLatinChars(input: String): Boolean = {
    for (char <- input) {
      // safe chars: 0 to 126, 160 to 190, 8211 to 8230, 8482 
      if (((char.toInt > 126 && char.toInt < 160) ||
          (char.toInt > 190 && char.toInt < 8211)) &&
          (char.toInt != 8482)) {
        return false;
      }
    }
    return true;
  }

  def isEnglish(input: String): Boolean = {
    try {
      val detector = DetectorFactory.create();
      detector.append(input);
      detector.detect().equals("en")

    } catch {
      // if a langdetectexception is thrown, err on the side of leniency:
      case e: LangDetectException => return true;
    }
  }

  // returns true if input contains HTML content
  def containsHtml(input: String): Boolean = {
    input.contains("</") ||  // closing tags
    input.contains("/>") ||  // self-closing tags
    input.contains("<a href=") ||  // anchor tags
    input.matches(".*<.{1,4}>.*")  // opening tags, 1-4 chars
  }

  // returns true if input contains less than three words (detected by the
  // number of spaces in input)
  def tooShort(input: String): Boolean = {
    // split on whitespace and count what's left
    val split = input.split("\\s+");

    split.length < 3
  }

  // returns true if the input is too long: more than 300 chars.
  def tooLong(input: String): Boolean = {
    input.length > 500
  }
}
