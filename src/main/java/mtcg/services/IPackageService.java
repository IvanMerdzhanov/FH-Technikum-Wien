package mtcg.services;

import mtcg.models.Card;
import java.util.List;
import java.util.UUID;

public interface IPackageService {
    List<Card> getPackageCards();
    void setCardAsNotTaken(UUID cardId);
    Card getCardById(UUID cardId);
}
