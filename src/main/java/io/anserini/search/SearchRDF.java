package io.anserini.search;

import io.anserini.index.generator.LuceneRDFDocumentGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CheckHits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.OptionHandlerFilter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Search an indexed RDF collection.
 *
 * This should simulate some conjunctive queries that
 * can be expressed using SPARQL.
 */
public class SearchRDF implements Closeable {
  private static final Logger LOG = LogManager.getLogger(SearchRDF.class);

  private final IndexReader reader;

  static final class Args {
    // Required arguments

    @Option(name = "-index", metaVar = "[Path]", required = true, usage = "index path")
    public String index;

    @Option(name = "-subject", metaVar = "[Subject URI]", required =  true, usage = "entity subject URI")
    public String subject;

    // Optional arguments

    @Option(name = "-predicate", metaVar = "[Predicate name]", usage = "predicate name to return")
    String predicate;
  }

  private SearchRDF(String indexDir) throws IOException {
    // Initialize index reader
    LOG.info("Reading index from " + indexDir);

    Path indexPath = Paths.get(indexDir);

    if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
      throw new IllegalArgumentException(indexDir + " does not exist or is not a directory.");
    }

    this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  /**
   * Prints query results to the standard output stream.
   *
   * @param subject the subject to search
   * @param predicate the predicate to search, or null to print all properties
   * @throws Exception on error
   */
  public void search(String subject, String predicate) throws Exception {
    LOG.info("Querying started...");

    // Initialize index searcher
    IndexSearcher searcher = new IndexSearcher(reader);

    // Search for exact subject URI
    TermQuery query = new TermQuery(new Term(LuceneRDFDocumentGenerator.FIELD_SUBJECT, subject));

    // Collect all matching lucene document ids
    Set<Integer> matchingDocIds = new HashSet<>(5);
    searcher.search(query, new CheckHits.SetCollector(matchingDocIds));

    if (matchingDocIds.size() == 0) { // We couldn't find any matching documents
      String msg = "Cannot find subject: " + query;
      LOG.warn(msg);
    } else {
      // Retrieve and print documents
      matchingDocIds.forEach(luceneDocId -> {
        try {
          Document doc = reader.document(luceneDocId);
          doc.iterator().forEachRemaining(field -> {
            if (predicate == null || field.name().equals(predicate)) {
              String fieldMessage = field.name() + "\t:\t " + field.stringValue();
              LOG.info(fieldMessage);
            }
          });
        } catch (IOException e) {
          LOG.error("Error retrieving lucene document: {}", luceneDocId, e);
        }
      });
    }

    LOG.info("Querying completed.");
  }

  public static void main(String[] args) throws Exception {
    Args searchArgs = new Args();

    // Parse args
    CmdLineParser parser = new CmdLineParser(searchArgs,
            ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example command: "+ SearchRDF.class.getSimpleName() +
              parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    new SearchRDF(searchArgs.index).search(searchArgs.subject, searchArgs.predicate);
  }
}
