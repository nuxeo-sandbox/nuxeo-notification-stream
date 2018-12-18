package org.nuxeo.ecm.notification.entities.formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.notification.entities.TextEntity;
import org.nuxeo.ecm.notification.entities.TextEntityFormatter;

public class DefaultTextEntityFormatter implements TextEntityFormatter {
    @Override
    public String format(String message, List<TextEntity> entities) {
        final String[] txt = { message };
        List<TextEntity> reversed = new ArrayList<>(entities);
        Collections.reverse(reversed);

        reversed.forEach(e -> txt[0] = txt[0].substring(0, e.getStart()) + e.getTitle() + txt[0].substring(e.getEnd()));
        return txt[0];
    }
}
