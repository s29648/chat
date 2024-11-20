package client.commands;

import client.GUIManager;
import client.messages.ExcludeRecipientsMessage;

public final class ExcludedMessageGUICommand extends GUICommand {
    private final ExcludeRecipientsMessage message;

    public ExcludedMessageGUICommand(ExcludeRecipientsMessage message, GUIManager guiManager) {
        super(guiManager);
        this.message = message;
    }

    @Override
    public void execute() {
        String currentUsername = guiManager.getCurrentUsername();

        if (!message.getExcludedRecipients().contains(currentUsername)) {
            guiManager.addMessageToChat(message.getSender(), message.getContent(), message.getTimestamp());
        }
    }
}