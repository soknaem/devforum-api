package com.setec.forum.feature.media.service;

import com.setec.forum.feature.media.*;
import com.setec.forum.feature.media.dto.response.MediaResponse;
import com.setec.forum.model.Media;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaResponse uploadStandalone(MultipartFile file, String subFolder, String currentUsername);
    MediaResponse uploadForResource(Long resourceId, MultipartFile file, String currentUsername);
    Media attachToResource(Resource resource, MultipartFile file, User uploader);
    void deleteFile(Media media);
    List<MediaResponse> getForResource(Long resourceId);
    void delete(Long mediaId, String currentUsername);
}
