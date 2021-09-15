package cuchaz.enigma.translation.mapping.serde.jadx;

import com.google.common.base.Charsets;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingPair;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsReader;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public enum JadxMappingsReader implements MappingsReader {
    INSTANCE;

    @Override
    public EntryTree<EntryMapping> read(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
        return read(path, Files.readAllLines(path, Charsets.UTF_8), progress);
    }

    private EntryTree<EntryMapping> read(Path path, List<String> lines, ProgressListener progress) throws MappingParseException {
        EntryTree<EntryMapping> mappings = new HashEntryTree<>();
        lines.remove(0);

        progress.init(lines.size(), I18n.translate("progress.mappings.jadx_file.loading"));

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            progress.step(lineNumber, "");

            String line = lines.get(lineNumber);

            if (line.trim().startsWith("#") || line.trim().startsWith("p")) {
                continue;
            }

            try {
                MappingPair<?, EntryMapping> mapping = parseLine(line);
                mappings.insert(mapping.getEntry(), mapping.getMapping());
            } catch (Throwable t) {
                t.printStackTrace();
                throw new MappingParseException(path::toString, lineNumber, t.toString());
            }
        }

        return mappings;
    }

    private MappingPair<?, EntryMapping> parseLine(String line) {
        String[] tokens = line.split(" ");

        String key = tokens[0];
        return switch (key) {
            case "c" -> parseClass(tokens);
            case "f" -> parseField(tokens);
            case "m" -> parseMethod(tokens);
            default -> throw new RuntimeException("Unknown token '" + key + "'!");
        };
    }

    private MappingPair<ClassEntry, EntryMapping> parseClass(String[] tokens) {
        // c <owner>.<src> = <dst>
        String ownerClass = tokens[1].replaceAll("\\.", "/");
        ClassEntry obfuscatedEntry = new ClassEntry(ownerClass);
        String mapping = obfuscatedEntry.getPackageName() + "/" + tokens[3];

        return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
    }

    private MappingPair<FieldEntry, EntryMapping> parseField(String[] tokens) {
        // f <owner>.<src>:<sig> = <dst>
        int ownerSep = tokens[1].lastIndexOf(".");
        int descriptorSep = tokens[1].lastIndexOf(":") + 1;
        ClassEntry ownerClass = new ClassEntry(tokens[1].substring(0, ownerSep).replaceAll("\\.", "/"));
        TypeDescriptor descriptor = new TypeDescriptor(tokens[1].substring(descriptorSep));

        String obfuscatedField = tokens[1].substring(ownerSep + 1, descriptorSep - 1);
        FieldEntry obfuscatedEntry = new FieldEntry(ownerClass, obfuscatedField, descriptor);
        String mapping = tokens[3];
        return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
    }

    private MappingPair<MethodEntry, EntryMapping> parseMethod(String[] tokens) {
        // m <owner>.<src><sig> = <dst>
        int ownerSep = tokens[1].lastIndexOf(".");
        int sigSep = tokens[1].indexOf("("); // Include "(" in output

        ClassEntry ownerClass = new ClassEntry(tokens[1].substring(0, ownerSep).replaceAll("\\.", "/"));
        MethodDescriptor descriptor = new MethodDescriptor(tokens[1].substring(sigSep));

        String obfuscatedMethod = tokens[1].substring(ownerSep + 1, sigSep);
        MethodEntry obfuscatedEntry = new MethodEntry(ownerClass, obfuscatedMethod, descriptor);
        String mapping = tokens[3];
        return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
    }
}
