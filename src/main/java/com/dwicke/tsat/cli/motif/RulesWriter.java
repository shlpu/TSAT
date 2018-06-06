package com.dwicke.tsat.cli.motif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.seninp.gi.logic.GrammarRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Write GrammarRules to an output file.
 */
public class RulesWriter {
    private static final Logger logger = LoggerFactory.getLogger(RulesWriter.class);
    private static final String NEWLINE = System.lineSeparator();

    private String fname;
    private BufferedWriter writer;

    public RulesWriter(String fname) {
        this.fname = fname;
        this.writer = null;
    }

    public RulesWriter write(GrammarRules rules) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.fname)))) {
            this.writer = writer;
            this.writeHeader()
                    .writeRules(rules);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.error("Encountered error while writing stats file: " + e.getMessage(), e);
            } else {
                logger.error("Encountered error while writing stats file: " + e.getMessage());
            }
        }
        return this;
    }

    private RulesWriter writeHeader() throws IOException {
        return this;
    }

    private void writeRules(GrammarRules rules) throws IOException {
        Gson g = new GsonBuilder().serializeSpecialFloatingPointValues().create();

        writer.write(g.toJson(rules));

    }
}
