package io.github.chrisruffalo.pintle.resolution.action;

import io.github.chrisruffalo.pintle.config.ActionList;
import org.xbill.DNS.Name;

import java.util.List;
import java.util.Optional;

public interface Act {

    /**
     * Reports the result of asking if an action should be
     * taken about the named being queried for based
     * on the content of the action lists.
     *
     * @param queryName dns name that is being queried
     * @param lists all the action lists to check
     * @return an action result (what was matched) if an action should be taken, an empty optional otherwise
     */
    Optional<ActionResult> on(final String configId, final Name queryName, final List<ActionList> lists);

}
