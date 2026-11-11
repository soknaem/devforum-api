package com.setec.forum.init;

import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.resource.CategoryRepository;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.TagRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.Category;
import com.setec.forum.model.Comment;
import com.setec.forum.model.Reaction;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.ReactionType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import com.setec.forum.model.Role;
import com.setec.forum.model.Tag;
import com.setec.forum.model.User;
import com.setec.forum.model.UserProfile;
import net.datafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Seeds enough demo data (users, categories, tags, resources, comments, reactions) to exercise
 * every endpoint - the feed, pagination, comment threads, and reaction counts all need more than
 * one or two rows to be worth looking at. Uses datafaker for realistic-looking filler content;
 * only runs when the relevant tables are empty, so it's safe on every restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ResourceRepository resourceRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final PasswordEncoder passwordEncoder;

    private final Faker faker = new Faker(Locale.ENGLISH);
    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        List<Category> categories = seedCategories();
        List<Tag> tags = seedTags();
        List<User> users = seedUsers();
        List<Resource> resources = seedResources(users, categories, tags);
        seedCommentsAndReactions(users, resources);
    }

    private List<Category> seedCategories() {
        if (categoryRepository.count() > 0) {
            return categoryRepository.findAll();
        }

        String[][] categoryData = {
                {"Books", "Recommended technical and educational books"},
                {"Code Snippets", "Reusable code samples and gists"},
                {"Videos", "Tutorial and conference video links"},
                {"Articles", "Blog posts and long-form articles"},
                {"Tools", "Developer tools and utilities"}
        };

        List<Category> saved = new ArrayList<>();
        for (String[] c : categoryData) {
            saved.add(categoryRepository.save(Category.builder().name(c[0]).description(c[1]).build()));
        }
        log.info("Seeded {} categories", saved.size());
        return saved;
    }

    private List<Tag> seedTags() {
        if (tagRepository.count() > 0) {
            return tagRepository.findAll();
        }
        String[] tagNames = {"java", "spring-boot", "react", "nextjs", "python", "algorithms",
                "system-design", "databases", "career", "best-practices"};
        List<Tag> saved = new ArrayList<>();
        for (String name : tagNames) {
            saved.add(tagRepository.save(Tag.builder().name(name).build()));
        }
        log.info("Seeded {} tags", saved.size());
        return saved;
    }

    private List<User> seedUsers() {
        if (userRepository.count() > 0) {
            return userRepository.findAll();
        }

        List<User> saved = new ArrayList<>();

        User admin = User.builder()
                .username("admin")
                .email("admin@devforum.local")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ROLE_ADMIN)
                .build();
        admin.setProfile(UserProfile.builder().fullName("Forum Administrator").bio("Keeping the forum tidy.").build());
        saved.add(userRepository.save(admin));

        User demoUser = User.builder()
                .username("demo")
                .email("demo@devforum.local")
                .password(passwordEncoder.encode("Demo@123"))
                .role(Role.ROLE_USER)
                .build();
        demoUser.setProfile(UserProfile.builder().fullName("Demo User").bio("Just here to try things out.").build());
        saved.add(userRepository.save(demoUser));

        // A handful of extra fake users so follows/feed/comments have more than one voice
        for (int i = 0; i < 6; i++) {
            String username = faker.internet().username().replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase(Locale.ROOT);
            User user = User.builder()
                    .username(username + i)
                    .email(username + i + "@devforum.local")
                    .password(passwordEncoder.encode("Passw0rd!"))
                    .role(Role.ROLE_USER)
                    .build();
            user.setProfile(UserProfile.builder()
                    .fullName(faker.name().fullName())
                    .bio(faker.lorem().sentence())
                    .location(faker.address().city())
                    .build());
            saved.add(userRepository.save(user));
        }

        log.info("Seeded {} users (admin/Admin@123, demo/Demo@123, plus {} fake accounts)", saved.size(), saved.size() - 2);
        return saved;
    }

    private List<Resource> seedResources(List<User> users, List<Category> categories, List<Tag> tags) {
        if (resourceRepository.count() > 0) {
            return resourceRepository.findAll();
        }

        List<Resource> saved = new ArrayList<>();
        ResourceType[] types = ResourceType.values();

        for (int i = 0; i < 5  ; i++) {
            User author = users.get(random.nextInt(users.size()));
            Category category = categories.get(random.nextInt(categories.size()));
            ResourceType type = types[random.nextInt(types.length)];

            Set<Tag> resourceTags = new HashSet<>();
            int tagCount = 1 + random.nextInt(3);
            for (int t = 0; t < tagCount; t++) {
                resourceTags.add(tags.get(random.nextInt(tags.size())));
            }

            Resource resource = Resource.builder()
                    .title(faker.book().title())
                    .description(faker.lorem().paragraph(3))
                    .url(type == ResourceType.LINK || type == ResourceType.ARTICLE || type == ResourceType.VIDEO
                            ? faker.internet().url() : null)
                    .type(type)
                    .status(ResourceStatus.PUBLISHED)
                    .user(author)
                    .category(category)
                    .tags(resourceTags)
                    .build();

            saved.add(resourceRepository.save(resource));
        }

        log.info("Seeded {} demo resources", saved.size());
        return saved;
    }

    private void seedCommentsAndReactions(List<User> users, List<Resource> resources) {
        if (commentRepository.count() > 0 || reactionRepository.count() > 0) {
            return;
        }

        for (Resource resource : resources) {
            int commentCount = random.nextInt(4);
            for (int c = 0; c < commentCount; c++) {
                User commenter = users.get(random.nextInt(users.size()));
                Comment comment = Comment.builder()
                        .content(faker.lorem().sentence())
                        .resource(resource)
                        .user(commenter)
                        .build();
                commentRepository.save(comment);
            }

            int reactionCount = random.nextInt(users.size());
            ReactionType[] types = ReactionType.values();
            List<User> shuffled = new ArrayList<>(users);
            java.util.Collections.shuffle(shuffled, random);
            for (int r = 0; r < reactionCount; r++) {
                User reactor = shuffled.get(r);
                reactionRepository.save(Reaction.builder()
                        .user(reactor)
                        .targetType(ReactionTargetType.RESOURCE)
                        .targetId(resource.getId())
                        .type(types[random.nextInt(types.length)])
                        .build());
            }
        }

        log.info("Seeded comments and reactions across {} resources", resources.size());
    }
}
