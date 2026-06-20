package com.aresstack.askai.client;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response returned by Ollama /api/tags.
 */
public final class OllamaModelListResponse {

    private static final Gson GSON = new Gson();

    private final List<OllamaModelInfo> models;

    public OllamaModelListResponse(List<OllamaModelInfo> models) {
        this.models = Collections.unmodifiableList(new ArrayList<OllamaModelInfo>(models));
    }

    public static OllamaModelListResponse fromJson(String json) {
        TagsResponseDto response = GSON.fromJson(json, TagsResponseDto.class);
        ArrayList<OllamaModelInfo> models = new ArrayList<OllamaModelInfo>();
        if (response != null && response.models != null) {
            for (ModelDto dto : response.models) {
                if (dto != null) {
                    models.add(dto.toModelInfo());
                }
            }
        }
        return new OllamaModelListResponse(models);
    }

    public List<OllamaModelInfo> getModels() {
        return models;
    }

    private static final class TagsResponseDto {
        private List<ModelDto> models;
    }

    private static final class ModelDto {
        private String name;
        private String model;
        @SerializedName("modified_at")
        private String modifiedAt;
        private long size;
        private String digest;
        private DetailsDto details;

        private OllamaModelInfo toModelInfo() {
            return new OllamaModelInfo(name, model, modifiedAt, size, digest,
                    details == null ? OllamaModelDetails.empty() : details.toModelDetails());
        }
    }

    private static final class DetailsDto {
        private String format;
        private String family;
        @SerializedName("parameter_size")
        private String parameterSize;
        @SerializedName("quantization_level")
        private String quantizationLevel;

        private OllamaModelDetails toModelDetails() {
            return new OllamaModelDetails(format, family, parameterSize, quantizationLevel);
        }
    }
}
