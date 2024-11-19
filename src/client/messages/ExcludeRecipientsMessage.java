package client.messages;

import java.util.Set;

public final class ExcludeRecipientsMessage extends Message {
    private final String excludedRecipients;
    private final String sender;
    private final String content;

    public ExcludeRecipientsMessage(String excludedRecipients, String sender, String content) {
        this.excludedRecipients = excludedRecipients;
        this.sender = sender;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }
    public String getContent() {
        return content;
    }
    public Set<String> getExcludedRecipients() {
        if (excludedRecipients == null || excludedRecipients.isEmpty()) {
            return Set.of(); // Return an empty set if no recipients are excluded
        }
        return Set.of(excludedRecipients.split(","));
    }

}