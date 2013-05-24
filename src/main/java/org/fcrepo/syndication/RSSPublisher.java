
package org.fcrepo.syndication;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static org.fcrepo.utils.EventType.getEventType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.AbstractResource;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

@Component
@Path("/rss")
public class RSSPublisher extends AbstractResource {

    private static final Integer FEED_LENGTH = 10;

    private static final String FEED_TYPE = "rss_2.0";

    private static final String FEED_TITLE = "What's happening in Fedora";

    private static final String FEED_DESCRIPTION = FEED_TITLE;

    @Autowired
    private EventBus eventBus;

    private final BlockingQueue<Event> feedQueue =
            new ArrayBlockingQueue<Event>(FEED_LENGTH);

    private final SyndFeed feed = new SyndFeedImpl();

    @GET
    @Produces("application/rss+xml")
    public StreamSource getFeed() throws FeedException {
        feed.setLink(uriInfo.getBaseUri().toString());
        feed.setEntries(transform(copyOf(feedQueue).reverse(), event2entry));
        // TODO ought to make this stream, not go through a string
        return new StreamSource(new ByteArrayInputStream(new SyndFeedOutput()
                .outputString(feed).getBytes(StandardCharsets.UTF_8)));
    }

    private final Function<Event, SyndEntry> event2entry =
            new Function<Event, SyndEntry>() {

                @Override
                public SyndEntry apply(final Event event) {
                    final SyndEntry entry = new SyndEntryImpl();
                    try {
                        entry.setTitle(event.getIdentifier());
                        entry.setLink(event.getPath());
                        entry.setPublishedDate(new DateTime(event.getDate())
                                .toDate());
                        final SyndContent description = new SyndContentImpl();
                        description.setType("text/plain");
                        description.setValue(getEventType(event.getType())
                                .toString());
                        entry.setDescription(description);
                    } catch (final RepositoryException e) {
                        throw propagate(e);
                    }
                    return entry;
                }

            };

    @Override
    @PostConstruct
    public void initialize() {
        eventBus.register(this);
        feed.setFeedType(FEED_TYPE);
        feed.setTitle(FEED_TITLE);
        feed.setDescription(FEED_DESCRIPTION);
    }

    @Subscribe
    public void newEvent(final Event event) {
        if (feedQueue.remainingCapacity() > 0) {
            feedQueue.offer(event);
        } else {
            feedQueue.poll();
            feedQueue.offer(event);
        }
    }

}