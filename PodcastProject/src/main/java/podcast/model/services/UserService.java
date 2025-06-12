 package podcast.model.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import podcast.model.entities.Podcast;
import podcast.model.entities.User;
import podcast.model.entities.dto.PodcastDTO;
import podcast.model.entities.dto.UpdateUserDTO;
import podcast.model.entities.dto.UserDTO;
import podcast.model.entities.enums.Role;
import podcast.model.exceptions.AlreadyCreatedException;
import podcast.model.exceptions.PodcastNotFoundException;
import podcast.model.exceptions.UserNotFoundException;
import podcast.model.repositories.interfaces.IPodcastRepository;
import podcast.model.repositories.interfaces.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    // ── Inyeccion De Dependencias Necesarias ─────────────────────────────────────────

    private final IUserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final IPodcastRepository podcastRepository;

    // ── Constructor ──────────────────────────────────────────────────────────────────

    @Autowired
    public UserService(IUserRepository userRepository, PasswordEncoder passwordEncoder, IPodcastRepository podcastRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.podcastRepository = podcastRepository;
    }

    // ── Logica De Negocio ────────────────────────────────────────────────────────────


    // ── Get ──────────────────────────────────────────────────────────────────────────

    public User getAuthenticatedUser(String username) {
        return userRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con username: " + username));
    }

    public List<UserDTO> getAllUsersAsDTO() {
        return userRepository.findAll()
                .stream()
                .map(User::toDTO)
                .toList();
    }

    public UserDTO getUserByIdAsDTO(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
        return user.toDTO();
    }

    public User getUserWithCredentialsById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
    }

    public List<PodcastDTO> getFavoritesByUsername(String username) {
        User user = userRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con username: " + username));
        return user.getFavorites().stream().map(Podcast::toDTO).toList();
    }

    // ── Post ─────────────────────────────────────────────────────────────────────────

    public void save(User user) {
        // Verifica que el id sea nulo pq es autoincremental en la bdd
        if (user.getId() != null) {
            throw new AlreadyCreatedException("No se debe enviar un ID al registrar un usuario nuevo");
        }
        // Asigna rol por defecto si no tiene
        user.getCredential().getRoles().clear();
        user.getCredential().getRoles().add(Role.ROLE_USER);
        // Encripta la contraseña
        String rawPassword = user.getCredential().getPassword();
        user.getCredential().setPassword(passwordEncoder.encode(rawPassword));

        // Asigna fecha de creación si es nuevo
        if (user.getCredential().getCreatedAt() == null) {
            user.getCredential().setCreatedAt(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByCredentialUsername(username);
    }

    public void addPodcastToFavorites(String username, Long podcastId) {
        User user = userRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con username: " + username));
        Podcast podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new PodcastNotFoundException("Podcast no encontrado con id: " + podcastId));

        if (user.getFavorites().contains(podcast)) {
            throw new IllegalArgumentException("El podcast ya está en la lista de favoritos");
        }

        user.getFavorites().add(podcast);
        userRepository.save(user);
    }

    // ── Patch ────────────────────────────────────────────────────────────────────────

    public User updateAuthenticatedUser(String username, UpdateUserDTO updates) {
        User existingUser = userRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con username: " + username));

        // Actualiza solo los campos proporcionados y no vacíos
        if (updates.getNickname() != null && !updates.getNickname().isBlank()) {
            existingUser.setNickname(updates.getNickname());
        }
        if (updates.getProfilePicture() != null && !updates.getProfilePicture().isBlank()) {
            existingUser.setProfilePicture(updates.getProfilePicture());
        }
        if (updates.getBio() != null && !updates.getBio().isBlank()) {
            existingUser.setBio(updates.getBio());
        }
        if (updates.getEmail() != null && !updates.getEmail().isBlank()) {
            existingUser.getCredential().setEmail(updates.getEmail());
        }
        if (updates.getPassword() != null && !updates.getPassword().isBlank()) {
            existingUser.getCredential().setPassword(passwordEncoder.encode(updates.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    // ── Delete ───────────────────────────────────────────────────────────────────────

    public void deleteAuthenticatedUser(String username) {
        User user = userRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con username: " + username));

        boolean isOwnerOfPodcasts = podcastRepository.existsByUserId(user.getId());
        if (isOwnerOfPodcasts) {
            throw new IllegalArgumentException("No se puede eliminar el usuario porque es dueño de uno o más podcasts.");
        }

        userRepository.delete(user);
    }

    public void removePodcastFromFavorites(String username, Long podcastId) {
        User user = userRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con username: " + username));
        Podcast podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new PodcastNotFoundException("Podcast no encontrado con id: " + podcastId));

        if (!user.getFavorites().contains(podcast)) {
            throw new IllegalArgumentException("El podcast no está en la lista de favoritos");
        }

        user.getFavorites().remove(podcast);
        userRepository.save(user);
    }
}