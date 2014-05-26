package at.yawk.data.xml.lexer;

import java.io.IOError;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Lexer that can be used to gradually read through an XML document.
 * <p/>
 * This lexer consists of a reader and a public interface. The reader adds new events to the event queue and you can use
 * the public methods to walk through that event queue.
 *
 * @author Yawkat
 */
public class BlockingLexer {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private Optional<Event> last = Optional.empty();

    /**
     * Read the input from the given reader and InputSource and add it to the event queue. This is a blocking operation.
     * If you chose to do this synchronously, all other methods will work but this method will only finished once the
     * entire document is read.
     */
    public void digest(XMLReader reader, InputSource input) throws IOException, SAXException {
        reader.setContentHandler(createContentHandler());
        reader.parse(input);
    }

    /**
     * Like #digest, but asynchronous on the given executor. This allows you to start walking the events while they are
     * still being read.
     */
    public void digestParallel(XMLReader reader, InputSource input, Executor executor) {
        executor.execute(() -> {
            try {
                digest(reader, input);
            } catch (IOException | SAXException e) {
                eventQueue.offer(new ExceptionEvent(e));
            }
        });
    }

    /**
     * Create a SAX content handler that feeds events to the event queue of this lexer.
     */
    // make public?
    private ContentHandler createContentHandler() {
        return new DefaultHandler() {
            @Override
            public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
                final Map<String, String> attributes = new HashMap<>(atts.getLength());
                for (int i = 0; i < atts.getLength(); i++) {
                    attributes.put(atts.getQName(i), atts.getValue(i));
                }
                eventQueue.offer(new ElementStartEvent(qName, Collections.unmodifiableMap(attributes)));
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                eventQueue.offer(new TextEvent(new String(ch, start, length)));
            }

            @Override
            public void endElement(String namespaceURI, String localName, String qName) {
                eventQueue.offer(new ElementEndEvent(qName));
            }

            @Override
            public void endDocument() {
                eventQueue.offer(FinishEvent.getInstance());
            }
        };
    }

    /**
     * Wait for a start element. Attributes are in key, value format: <code>("a", "href", "http://yawk.at")</code> would
     * match any links to <code>http://yawk.at</code>. Pattern objects are allowed as the values and will be treated as
     * such. All other objects will be converted to a string and searched for an ignore-case, exact match.
     *
     * @return the matched event or null if EOF is reached.
     */
    public ElementStartEvent start(String type, Object... attributes) {
        return start(type,
                     o -> o instanceof Pattern ?
                             (Pattern) o :
                             Pattern.compile(String.valueOf(o), Pattern.CASE_INSENSITIVE),
                     attributes
                    );
    }

    /**
     * Wait for a start element. Attributes are in key, value format: <code>("a", "href", "http://yawk.at")</code> would
     * match any links to <code>http://yawk.at</code>. The patternFactory converts the values of the attribute list to
     * Pattern objects.
     *
     * @return the matched event or null if EOF is reached.
     */
    public ElementStartEvent start(String type, Function<Object, Pattern> patternFactory, Object... attributes) {
        if (attributes.length % 2 != 0) {
            throw new IllegalArgumentException("Must supply an even amount of arguments.");
        }
        Map<String, Pattern> attributeMap = new HashMap<>(attributes.length / 2);
        for (int i = 0; i < attributes.length; i += 2) {
            attributeMap.put(String.valueOf(attributes[0]), patternFactory.apply(attributes[i + 1]));
        }
        return start(type, attributeMap);
    }

    /**
     * Wait for a start element. Attributes are in key, value format.
     *
     * @return the matched event or null if EOF is reached.
     */
    private ElementStartEvent start(String type, Map<String, Pattern> attributes) {
        return (ElementStartEvent) waitFor(event -> {
            if (!(event instanceof ElementStartEvent)) { return false; }
            ElementStartEvent start = (ElementStartEvent) event;
            if (!start.getTagName().equals(type)) { return false; }
            for (String k : attributes.keySet()) {
                if (!start.getAttributes().containsKey(k)) { return false; }
                if (!attributes.get(k).matcher(start.getAttributes().get(k)).matches()) { return false; }
            }
            return true;
        });
    }

    /**
     * Wait for the end of an element of the given type.
     *
     * @return the matched event or null if EOF is reached.
     */
    public ElementEndEvent end(String type) {
        return (ElementEndEvent) waitFor(event -> event instanceof ElementEndEvent &&
                                                  ((ElementEndEvent) event).getTagName().equals(type));
    }

    /**
     * Wait for the given text.
     *
     * @return the matched event or null if EOF is reached.
     */
    public TextEvent text(String text) {
        return text(Pattern.compile(text, Pattern.LITERAL));
    }

    /**
     * Wait for text that matches the given pattern.
     *
     * @return the matched event or null if EOF is reached.
     */
    public TextEvent text(Pattern match) {
        return (TextEvent) waitFor(event -> event instanceof TextEvent &&
                                            (match.matcher(((TextEvent) event).getText()).matches() ||
                                             match.matcher(((TextEvent) event).getText().trim()).matches()));
    }

    /**
     * Skip to the next event.
     *
     * @return the next event or null if EOF is reached.
     */
    public Event next() {
        return waitFor(e -> true);
    }

    /**
     * Get the text of the current TextEvent.
     */
    public String getText() {
        return ((TextEvent) last.get()).getText();
    }

    /**
     * Get the tag name of the current ElementEvent.
     */
    public String getTagName() {
        return ((ElementEvent) last.get()).getTagName();
    }

    /**
     * Get the attribute value of the given key (or null) of the current ElementStartEvent.
     */
    public String getAttribute(String key) {
        return ((ElementStartEvent) last.get()).getAttributes().get(key);
    }

    /**
     * Wait for the given condition and return the event that matches it.
     *
     * @return the matched event or null if EOF is reached.
     */
    public Event waitFor(Predicate<Event> condition) {
        try {
            return waitFor0(condition);
        } catch (InterruptedException ignored) {}
        return null;
    }

    private Event waitFor0(Predicate<Event> condition) throws InterruptedException {
        while (true) {
            Event event = eventQueue.take();
            if (event instanceof FinishEvent) {
                last = Optional.empty();
                return null;
            }
            if (event instanceof ExceptionEvent) { throw new IOError(((ExceptionEvent) event).getException()); }
            if (condition.test(event)) {
                last = Optional.of(event);
                return event;
            }
        }
    }
}
