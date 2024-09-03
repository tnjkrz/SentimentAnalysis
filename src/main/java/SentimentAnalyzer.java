import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;

import java.util.*;

public class SentimentAnalyzer {
    private final StanfordCoreNLP pipeline;

    public SentimentAnalyzer() {
        // setting up CoreNLP pipeline with required annotators
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse,sentiment");
        pipeline = new StanfordCoreNLP(props);
    }

    public List<String[]> analyzeData(List<String[]> cleanedData) {
        List<String[]> analyzedData = new ArrayList<>();

        for (String[] data : cleanedData) {
            String reviewText = data[3]; // reviewText is the 4th element in the array

            // annotate review text
            Annotation annotation = new Annotation(reviewText);
            pipeline.annotate(annotation);

            // extract sentiment for whole review
            int sentiment = extractSentiment(annotation);

            // extract topics (nouns) and adjectives
            List<String> topics = new ArrayList<>();
            List<String> adjectives = new ArrayList<>();
            extractTopicsAndAdjectives(annotation, topics, adjectives);

            // handle scenario where no topics/adjectives are found
            String topicsStr;
            if (topics.isEmpty()) {
                topicsStr = "NO_TOPICS";
            } else {
                topicsStr = String.join(", ", topics);
            }

            String adjectivesStr;
            if (adjectives.isEmpty()) {
                adjectivesStr = "NO_ADJECTIVES";
            } else {
                adjectivesStr = String.join(", ", adjectives);
            }

            // combining all extracted data into a new array
            String[] analyzedArray = new String[]{
                    data[0],                    // category
                    data[1],                    // overall (score)
                    data[2],                    // asin
                    String.valueOf(sentiment),  // sentiment score (1-5)
                    topicsStr,                  // topics/nouns
                    adjectivesStr               // adjectives
            };

            analyzedData.add(analyzedArray);
        }

        return analyzedData;
    }

    private int extractSentiment(Annotation annotation) {
        // extract sentiment value (0-4) for the sentences within the review
        int sentimentScore = 0;
        int totalSentences = 0;

        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            sentimentScore += RNNCoreAnnotations.getPredictedClass(tree);
            totalSentences++;
        }
        // average sentiment over all sentences, add +1 for (1-5) range
        return Math.round((float) sentimentScore / totalSentences) + 1;
    }

    private void extractTopicsAndAdjectives(Annotation annotation, List<String> topics, List<String> adjectives) {
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

                String word = token.word().trim(); // trim whitespace
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

                if (pos.startsWith("NN")) {         // matches NN, NNP, NNPS -> nouns
                    topics.add(word);
                } else if (pos.startsWith("JJ")) {  // matches JJ, JJR, JJS -> adjectives
                    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class).trim();
                    if (!lemma.isEmpty() && !lemma.equals(",")) {
                        adjectives.add(lemma);
                    }
                }
            }
        }
    }
}
