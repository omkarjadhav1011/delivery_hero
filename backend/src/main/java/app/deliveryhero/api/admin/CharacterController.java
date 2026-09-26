package app.deliveryhero.api.admin;

import app.deliveryhero.common.Role;
import app.deliveryhero.content.CharacterInput;
import app.deliveryhero.content.CharacterService;
import app.deliveryhero.content.CharacterView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The character endpoints (API section 7.5). */
@RestController
@RequestMapping("/api/admin/characters")
class CharacterController {

    private final CharacterService characters;

    CharacterController(CharacterService characters) {
        this.characters = characters;
    }

    @PutMapping("/{role}")
    CharacterView update(@PathVariable Role role, @RequestBody CharacterInput input) {
        return characters.update(role, input);
    }
}
