package cuchaz.enigma.translation.mapping.serde.jadx;

import com.google.common.collect.Lists;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.LfPrintWriter;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public enum JadxMappingsWriter implements MappingsWriter {
    INSTANCE;

    @Override
    public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
        try {
            Files.deleteIfExists(path);
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<String> packageLines = new TreeSet<>();
        List<String> classLines = new ArrayList<>();
        List<String> fieldLines = new ArrayList<>();
        List<String> methodLines = new ArrayList<>();

        List<? extends Entry<?>> rootEntries = Lists.newArrayList(mappings).stream()
                .map(EntryTreeNode::getEntry)
                .toList();
        progress.init(rootEntries.size(), I18n.translate("progress.mappings.jadx_file.generating"));

        int steps = 0;
        for (Entry<?> entry : sorted(rootEntries)) {
            progress.step(steps++, entry.getName());
            writeEntry(packageLines, classLines, fieldLines, methodLines, mappings, entry);
        }

        progress.init(3, I18n.translate("progress.mappings.jadx_file.writing"));
        try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
            progress.step(0, I18n.translate("type.packages"));
            packageLines.forEach(writer::println);
            progress.step(1, I18n.translate("type.classes"));
            classLines.forEach(writer::println);
            progress.step(2, I18n.translate("type.fields"));
            fieldLines.forEach(writer::println);
            progress.step(3, I18n.translate("type.methods"));
            methodLines.forEach(writer::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeEntry(Set<String> packages, List<String> classes, List<String> fields, List<String> methods, EntryTree<EntryMapping> mappings, Entry<?> entry) {
        EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
        if (node == null) {
            return;
        }

        Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);
        if (entry instanceof ClassEntry classEntry) {
            classes.add(generateClassLine(classEntry, translator));

            String targetPackageName = translator.translate(classEntry).getPackageName();
            int separator = targetPackageName.lastIndexOf("/") + 1;
            String packageName = targetPackageName.substring(separator).replaceAll("/", ".");

            packages.add("p " + getName(classEntry.getPackageName()) + " = " + packageName);
        } else if (entry instanceof FieldEntry) {
            fields.add(generateFieldLine((FieldEntry) entry, translator));
        } else if (entry instanceof MethodEntry) {
            methods.add(generateMethodLine((MethodEntry) entry, translator));
        }

        for (Entry<?> child : sorted(node.getChildren())) {
            writeEntry(packages, classes, fields, methods, mappings, child);
        }
    }

    private String getName(Entry<?> entry) {
        return entry.getName().replaceAll("/", ".");
    }

    private String getName(String name) {
        return name.replaceAll("/", ".");
    }

    private String generateClassLine(ClassEntry sourceEntry, Translator translator) {
        ClassEntry targetEntry = translator.translate(sourceEntry);
        return "c " + getName(sourceEntry) + " = " + targetEntry.getSimpleName();
    }

    private String generateMethodLine(MethodEntry sourceEntry, Translator translator) {
        MethodEntry targetEntry = translator.translate(sourceEntry);
        return "m " + getName(sourceEntry.getParent()) + "." + getName(sourceEntry) + sourceEntry.getDesc() + " = " + getName(targetEntry);
    }

    private String generateFieldLine(FieldEntry sourceEntry, Translator translator) {
        FieldEntry targetEntry = translator.translate(sourceEntry);
        return "f " + getName(sourceEntry.getParent()) + "." + getName(sourceEntry) + ":" + sourceEntry.getDesc() + " = " + getName(targetEntry);
    }

    private Collection<Entry<?>> sorted(Iterable<? extends Entry<?>> iterable) {
        ArrayList<Entry<?>> sorted = Lists.newArrayList(iterable);
        sorted.sort(Comparator.comparing(Entry::getName));
        return sorted;
    }
}
