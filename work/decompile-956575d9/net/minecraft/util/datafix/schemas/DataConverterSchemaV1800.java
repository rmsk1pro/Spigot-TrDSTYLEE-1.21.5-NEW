package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV1800 extends DataConverterSchemaNamed {

    public DataConverterSchemaV1800(int i, Schema schema) {
        super(i, schema);
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);

        schema.registerSimple(map, "minecraft:panda");
        schema.register(map, "minecraft:pillager", (s) -> {
            return DSL.optionalFields("Inventory", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
        return map;
    }
}
