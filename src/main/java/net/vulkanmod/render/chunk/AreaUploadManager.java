package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import static net.vulkanmod.vulkan.queue.Queue.TransferQueue;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import org.lwjgl.vulkan.VkBufferCopy;

import static net.vulkanmod.vulkan.queue.Queue.FakeTransferQueue;

public class AreaUploadManager {
    public static final int FRAME_NUM = 2;
    public static AreaUploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new AreaUploadManager();
    }

    ObjectArrayList<AreaBuffer.Segment>[] recordedUploads;
    CommandPool.CommandBuffer[] commandBuffers;

//    LongOpenHashSet dstBuffers = new LongOpenHashSet();

    Long2ObjectArrayMap<ObjectArrayFIFOQueue<SubCopyCommand>> subCopyCommands = new Long2ObjectArrayMap<>();

    int currentFrame;

    public void init() {
        this.commandBuffers = new CommandPool.CommandBuffer[FRAME_NUM];
        this.recordedUploads = new ObjectArrayList[FRAME_NUM];

        for (int i = 0; i < FRAME_NUM; i++) {
            this.recordedUploads[i] = new ObjectArrayList<>();
        }
    }

    public void swapBuffers(long srcBuffer, long dstBuffer)
    {        hasBufferSwap=true;
        if(!this.subCopyCommands.containsKey(srcBuffer)) return;
        this.subCopyCommands.put(dstBuffer, this.subCopyCommands.remove(srcBuffer));


    }
    public synchronized void submitUploads() {
        if (subCopyCommands.isEmpty()) {
            return;

        TransferQueue.submitCommands(this.commandBuffers[currentFrame]);
    }

    public void uploadAsync(AreaBuffer.Segment uploadSegment, long bufferId, long dstOffset, long bufferSize, ByteBuffer src) {

        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = TransferQueue.beginCommands();

        VkCommandBuffer commandBuffer = commandBuffers[currentFrame].getHandle();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.copyBuffer(bufferSize, src);


        if(!subCopyCommands.containsKey(bufferId))
        {
            subCopyCommands.put(bufferId, new ObjectArrayFIFOQueue<>(12));
        }
        subCopyCommands.get(bufferId).enqueue(new SubCopyCommand(stagingBuffer.getOffset(), dstOffset, bufferSize));


        this.recordedUploads[this.currentFrame].add(uploadSegment);
    }

    public void updateFrame() {
        this.currentFrame = (this.currentFrame + 1) % FRAME_NUM;
        waitUploads(this.currentFrame);

    }

    public void waitUploads() {
        this.waitUploads(this.currentFrame);
    }
    private void waitUploads(int frame) {
        CommandPool.CommandBuffer commandBuffer = commandBuffers[frame];
        if(commandBuffer == null)
            return;
        Synchronization.waitFence(commandBuffers[frame].getFence());

        for(AreaBuffer.Segment uploadSegment : this.recordedUploads[frame]) {
            uploadSegment.setReady();
        }

        this.commandBuffers[frame].reset();
        this.commandBuffers[frame] = null;
        this.recordedUploads[frame].clear();
    }

    public synchronized void waitAllUploads() {
        for(int i = 0; i < this.commandBuffers.length; ++i) {
            waitUploads(i);
        }
    }

    public void copyBuffer(long srcBuffer, long dstBuffer, int bufferSize) {
        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = FakeTransferQueue.beginCommands();
        FakeTransferQueue.uploadBufferCmd(this.commandBuffers[currentFrame], srcBuffer, 0, dstBuffer, 0, bufferSize);
    }
}
