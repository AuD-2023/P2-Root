package p2.storage;

import java.util.Arrays;

class MultiIntervalView implements StorageView {

    private final /* immutable */ SizedViewTuple[] baseViews;
    private final int size;
    private final int[] cumulativeLength;

    MultiIntervalView(SizedViewTuple[] baseViews) {
        this.baseViews = baseViews;
        cumulativeLength = calculateCumulativeLength(baseViews);
        if (baseViews.length == 0) {
            size = 0;
        } else {
            size = cumulativeLength[cumulativeLength.length - 1] + 1;
        }
    }

    static int[] calculateCumulativeLength(SizedViewTuple[] views) {
        final int[] indices = new int[views.length];
        int last = 0;
        for (int i = 0; i < views.length; i++) {
            last += views[i].size();
            indices[i] = last;
        }
        return indices;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public byte get(int index) {

        if (index == 0) {
            throw new SegmentationFault(this, index);
        }

        int i = Arrays.binarySearch(cumulativeLength, index);

        return baseViews[i].view().get(index - cumulativeLength[i - 1]);

    }

    @Override
    public void forEachByte(ByteConsumer consumer) {
        int index = 0;
        for (SizedViewTuple baseView : baseViews) {
            final int start = index;
            final int end = index + baseView.size();
            for (int i = start; i < end; i++) {
                consumer.accept(i, i, baseView.view().get(i - start));
            }
            index = end;
        }
    }

    @Override
    public StorageView plus(StorageView other) {
        if (other instanceof MultiIntervalView otherTree) {
            // flatten resulting tree by adding other's children instead of the tree itself
            final SizedViewTuple[] baseViews = new SizedViewTuple[this.baseViews.length + otherTree.getChildren().length];
            System.arraycopy(this.baseViews, 0, baseViews, 0, this.baseViews.length);
            System.arraycopy(otherTree.getChildren(), 0, baseViews, this.baseViews.length, otherTree.getChildren().length);
            return new MultiIntervalView(baseViews);
        }
        final SizedViewTuple[] baseViews = new SizedViewTuple[this.baseViews.length + 1];
        System.arraycopy(this.baseViews, 0, baseViews, 0, this.baseViews.length);
        baseViews[this.baseViews.length] = SizedViewTuple.ofEntire(other);
        return new MultiIntervalView(baseViews);
    }

    @Override
    public StorageView slice(StorageInterval interval) {
        return null;
    }

    SizedViewTuple[] getChildren() {
        return baseViews;
    }
}
