package io.anserini.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A document that represent an entity with multiple
 * RDF triples for the same entity subject URI.
 */
public class RDFDocument implements SourceDocument {

  /**
   * Splitter that describes how s,p,o are split in a triple line
   */
  public static final String TRIPLE_SPLITTER = "\t";

  /**
   * Subject of the triples doc, also the RDFDocument id
   */
  private String subject;

  /**
   * The predicates and values of the subject entity
   */
  private Map<String, List<String>> predicateValues = new TreeMap<>();

  /**
   * Constructor for an NT triples (NTriples).
   *
   * @param s subject
   * @param p predicate
   * @param o object
   */
  public RDFDocument(String s, String p, String o) {
    init(s, p, o);
  }

  /**
   * Clone from another document
   * @param other
   */
  public RDFDocument(RDFDocument other) {
    this.subject = other.subject;
    other.predicateValues.forEach((predicate, values) -> {
      this.predicateValues.put(predicate, new ArrayList<>(values));
    });
  }

  /**
   * Constructor from a line
   * @param line line that contains triple information
   */
  public RDFDocument(String line) throws IllegalArgumentException {
    String[] pieces = line.split(TRIPLE_SPLITTER);
    if (pieces.length == 4) {
      init(pieces[0], pieces[1], pieces[2]);
    } else {
      throw new IllegalArgumentException("Cannot parse triple from line: " + line);
    }
  }

  /**
   * Assign values
   * @param s subject
   * @param p predicate
   * @param o object
   */
  private void init(String s, String p, String o) {
    this.subject = s;
    // Add the predicate and object as the first element in the list
    addPredicateAndValue(p, o);
  }

  /**
   * Add the predicate and its value in the predicateValues map
   * @param p predicate
   * @param o object value
   */
  public void addPredicateAndValue(String p, String o) {
    List<String> values = predicateValues.get(p);

    if (values == null) {
      values = new ArrayList<>();
      predicateValues.put(p, values);
    }

    values.add(o);
  }

  @Override
  public String id() {
    return subject;
  }

  @Override
  public String content() {
    return this.toString();
  }

  /**
   * Always index all triples
   * @return true
   */
  @Override
  public boolean indexable() {
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    predicateValues.forEach((predicate, values) -> {
      for (String value : values) {
        sb.append(subject).append(TRIPLE_SPLITTER)
                .append(predicate).append(TRIPLE_SPLITTER)
                .append(value).append(TRIPLE_SPLITTER).append(".\n");
      }
    });
    return sb.toString();
  }

  public String getSubject() {
    return subject;
  }

  public Map<String, List<String>> getPredicateValues() {
    return predicateValues;
  }

  /**
   * Clears resources
   */
  public void clear() {
    predicateValues.clear();
    subject = null;
    predicateValues = null;
  }
}
