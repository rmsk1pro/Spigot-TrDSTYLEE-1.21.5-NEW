package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class BlockEntitySignDoubleSidedEditableTextFix extends NamedEntityWriteReadFix {

    public static final List<String> FIELDS_TO_DROP = List.of("Text1", "Text2", "Text3", "Text4", "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4", "Color", "GlowingText");
    public static final String FILTERED_CORRECT = "_filtered_correct";
    private static final String DEFAULT_COLOR = "black";

    public BlockEntitySignDoubleSidedEditableTextFix(Schema schema, String s, String s1) {
        super(schema, true, s, DataConverterTypes.BLOCK_ENTITY, s1);
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> dynamic) {
        dynamic = dynamic.set("front_text", fixFrontTextTag(dynamic)).set("back_text", createDefaultText(dynamic)).set("is_waxed", dynamic.createBoolean(false)).set("_filtered_correct", dynamic.createBoolean(true));

        for (String s : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
            dynamic = dynamic.remove(s);
        }

        return dynamic;
    }

    private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> dynamic) {
        Dynamic<T> dynamic1 = LegacyComponentDataFixUtils.<T>createEmptyComponent(dynamic.getOps());
        List<Dynamic<T>> list = getLines(dynamic, "Text").map((optional) -> {
            return (Dynamic) optional.orElse(dynamic1);
        }).toList();
        Dynamic<T> dynamic2 = dynamic.emptyMap().set("messages", dynamic.createList(list.stream())).set("color", (Dynamic) dynamic.get("Color").result().orElse(dynamic.createString("black"))).set("has_glowing_text", (Dynamic) dynamic.get("GlowingText").result().orElse(dynamic.createBoolean(false)));
        List<Optional<Dynamic<T>>> list1 = getLines(dynamic, "FilteredText").toList();

        if (list1.stream().anyMatch(Optional::isPresent)) {
            dynamic2 = dynamic2.set("filtered_messages", dynamic.createList(Streams.mapWithIndex(list1.stream(), (optional, i) -> {
                Dynamic<T> dynamic3 = (Dynamic) list.get((int) i);

                return (Dynamic) optional.orElse(dynamic3);
            })));
        }

        return dynamic2;
    }

    private static <T> Stream<Optional<Dynamic<T>>> getLines(Dynamic<T> dynamic, String s) {
        return Stream.of(dynamic.get(s + "1").result(), dynamic.get(s + "2").result(), dynamic.get(s + "3").result(), dynamic.get(s + "4").result());
    }

    private static <T> Dynamic<T> createDefaultText(Dynamic<T> dynamic) {
        return dynamic.emptyMap().set("messages", createEmptyLines(dynamic)).set("color", dynamic.createString("black")).set("has_glowing_text", dynamic.createBoolean(false));
    }

    private static <T> Dynamic<T> createEmptyLines(Dynamic<T> dynamic) {
        Dynamic<T> dynamic1 = LegacyComponentDataFixUtils.<T>createEmptyComponent(dynamic.getOps());

        return dynamic.createList(Stream.of(dynamic1, dynamic1, dynamic1, dynamic1));
    }
}
