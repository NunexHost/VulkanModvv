package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

public class Vec2f extends Field {

    public Vec2f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        this.values = Uniforms.vec2f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.values, "Field name not found: " + this.fieldInfo.name);
    }

    void update() {
        MappedBuffer buffer = this.values.get();

        MemoryUtil.memPutFloat(this.basePtr, buffer.getFloat(0));
        MemoryUtil.memPutFloat(this.basePtr + 4, buffer.getFloat(4));
    }
}
