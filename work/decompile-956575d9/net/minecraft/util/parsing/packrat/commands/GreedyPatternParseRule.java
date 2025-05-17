package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public final class GreedyPatternParseRule implements Rule<StringReader, String> {

    private final Pattern pattern;
    private final DelayedException<CommandSyntaxException> error;

    public GreedyPatternParseRule(Pattern pattern, DelayedException<CommandSyntaxException> delayedexception) {
        this.pattern = pattern;
        this.error = delayedexception;
    }

    @Override
    public String parse(ParseState<StringReader> parsestate) {
        StringReader stringreader = parsestate.input();
        String s = stringreader.getString();
        Matcher matcher = this.pattern.matcher(s).region(stringreader.getCursor(), s.length());

        if (!matcher.lookingAt()) {
            parsestate.errorCollector().store(parsestate.mark(), this.error);
            return null;
        } else {
            stringreader.setCursor(matcher.end());
            return matcher.group(0);
        }
    }
}
