package com.setec.forum.feature.resource;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.resource.dto.request.BlockRequest;
import com.setec.forum.feature.resource.dto.request.ResourceRequest;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResourceService {

    ResourceResponse create(ResourceRequest request, String currentUsername);

    ResourceResponse getById(Long id);

    /** Same as getById, but also records the view against `viewerUsername` for history (pass null for anonymous). */
    ResourceResponse getById(Long id, String viewerUsername);

    /** Re-fetches and re-maps a resource WITHOUT counting it as a view - use after a side-effect (e.g. attaching media) to return fresh data. */
    ResourceResponse refresh(Long id);

    PageResponse<ResourceResponse> getAll(String search, Long categoryId, ResourceType type,
                                           ResourceStatus status, String tag,
                                           int page, int size, String sortBy, String sortDir, String viewerUsername);

    ResourceResponse update(Long id, ResourceRequest request, String currentUsername);

    void delete(Long id, String currentUsername);

    /** Publish / move to draft / archive / restore. Owner or admin only. */
    ResourceResponse changeStatus(Long id, ResourceStatus status, String currentUsername);

    /** The caller's own resources in one status (Drafts / Archived pages). */
    PageResponse<ResourceResponse> getMine(ResourceStatus status, int page, int size, String currentUsername);

    /** {published, draft, archived} counts for the sidebar. */
    Map<String, Long> countMine(String currentUsername);

    /**
     * Replaces the ordered body of a resource. MEDIA blocks point at an existing file (mediaId) or at a
     * new upload (fileIndex into {@code files}); attachments no longer used by any block are deleted.
     */
    ResourceResponse updateContent(Long id, List<BlockRequest> blocks, List<MultipartFile> files, String currentUsername);

    // ---- used by the nightly clean-up of archived resources (see ArchivePurgeScheduler) ----
    int backfillArchivedAt();

    List<Long> findExpiredArchivedIds(int retentionDays);

    /** Deletes one archived resource for good (no permission check: system job). */
    void purge(Long id);
}
