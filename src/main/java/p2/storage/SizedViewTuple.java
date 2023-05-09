package p2.storage;

record SizedViewTuple(int size, StorageView view) {

    static SizedViewTuple ofEntire(StorageView view) {
        return new SizedViewTuple(view.size(), view);
    }
}
