package az.code.tourapp.models;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.Objects;

public class Command extends BotCommand {
    String command;

    public Command(String command) {
        this.command = command;
    }

    public Command(String command, String description) {
        super(command, description);
        this.command = command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command1 = (Command) o;
        return Objects.equals(this.command, command1.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }
}
