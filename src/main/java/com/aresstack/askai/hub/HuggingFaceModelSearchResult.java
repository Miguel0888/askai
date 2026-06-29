package com.aresstack.askai.hub;

/**
 * One model repository returned by a Hugging Face Hub search, in AskAI domain terms.
 */
public final class HuggingFaceModelSearchResult {

    private final String repoId;
    private final String author;
    private final String task;
    private final String library;
    private final long downloads;
    private final long likes;
    private final boolean gated;

    public HuggingFaceModelSearchResult(String repoId, String author, String task, String library,
                                        long downloads, long likes, boolean gated) {
        this.repoId = repoId == null ? "" : repoId;
        this.author = author == null ? "" : author;
        this.task = task == null ? "" : task;
        this.library = library == null ? "" : library;
        this.downloads = downloads;
        this.likes = likes;
        this.gated = gated;
    }

    public String getRepoId() {
        return repoId;
    }

    public String getAuthor() {
        return author;
    }

    public String getTask() {
        return task;
    }

    public String getLibrary() {
        return library;
    }

    public long getDownloads() {
        return downloads;
    }

    public long getLikes() {
        return likes;
    }

    public boolean isGated() {
        return gated;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(repoId);
        builder.append("   ↓ ").append(downloads).append("   ♥ ").append(likes);
        if (!task.isEmpty()) {
            builder.append("   ").append(task);
        }
        if (gated) {
            builder.append("   [gated]");
        }
        return builder.toString();
    }
}
