Ce document vise à décrire l'architecture de la fonctionnalité permettant de lier son compte à un nouvel appareil: `Add a new device`. 

Pour bien comprendre la suite de ce document, voici quelques éléments de vocabulaire:

- `import side`: côté qui importe le compte
- `export side`: côté qui exporte son compte
- `token`: URI qui permet d'identifier un appareil sur la DHT.

## Machine à état

Le `daemon` gère la fonctionnalité à l'aide d'une machine à état.

L'évolution de l'état est communiqué aux clients qui présente ainsi l'interface correspondante.

Pour l'instant, cette machine à état est symétrique (`import side` et `export side`). Néanmoins selon le côté, certains états ne sont pas atteignables. 

Voici la machine à état:

| Numéro | Nom             | Utilisation (côté) | Signification                                                |
| ------ | --------------- | ------------------ | :----------------------------------------------------------- |
| 0      | Init            | Aucun              | État initial.                                                |
| 1      | Token available | Import uniquement  | Le `token` est disponible. Il s'agit du `token` permettant l'identification du nouvel appareil sur la DHT. Il peut être affiché sous forme de texte ou de QR code. |
| 2      | Connecting      | Export/Import      | La connexion est disponible en pair à pair.                  |
| 3      | Authenticating  | Export/Import      | Confirmation de l'identité du compte et de l'adresse de l'appareil. |
| 4      | In progress     | Export/Import      | Le transfert de l'archive est en cours.                      |
| 5      | Done            | Export/Import      | État final. Succès ou erreur.                                |

Associé à cette machine état peut être passé des `details`. Ces détails portent un complément d'information utile pour l'affichage.

Le type de `details` est une `map<String, String>`.

### Clés et valeurs de `details` pour `import side`

| Numéro | Nom             | Détails                                                      |
| ------ | --------------- | :----------------------------------------------------------- |
| 0      | Init            | Non applicable                                               |
| 1      | Token available | `token`: une URI de 59 caractères avec pour préfix `jami-auth://` |
| 2      | Connecting      | Pas de détails                                               |
| 3      | Authenticating  | `peer_id`: Jami ID du compte importé<br />`auth_scheme`:`{"", "none", "password"}`; Vide si pas de protection accepté<br />`auth_error`: `{"bad_password"}` |
| 4      | In progress     | Pas de détails                                               |
| 5      | Done            | `error`: `{"", "none", "network", "authentication"};` Vide si pas d'erreur accepté |

### Clés et valeurs de `details` pour `export side`

| Numéro | Nom             | Détails                                                      |
| ------ | --------------- | :----------------------------------------------------------- |
| 0      | Init            | Non applicable                                               |
| 1      | Token available | Non applicable                                               |
| 2      | Connecting      | Pas de détails                                               |
| 3      | Authenticating  | `peer_address`: Adresse IP du compte exportant               |
| 4      | In progress     | Pas de détails                                               |
| 5      | Done            | `error`: `{"", "none", "network", "authentication"};` Vide si pas d'erreur accepté |

## API entre le daemon et le client

### API pour `import side`

| Nom du signal                | Direction | Utilité                                                      |
| ---------------------------- | --------- | ------------------------------------------------------------ |
| addAccount                   | Sortant   | Annonce la volonté d'importer un compte. Doit porter la clé `Account.archiveURL="jami-auth"`. |
| provideAccountAuthentication | Sortant   | Renseigne le mot de passe.<br />Confirme l'identité du compte importé. |
| removeAccount                | Sortant   | Annuler l'opération.                                         |
| deviceAuthStateChanged       | Entrant   | Indique le nouvel état et porte les détails.                 |

### API pour `export side`

| Nom du signal         | Direction | Utilité                                      |
| --------------------- | --------- | -------------------------------------------- |
| addDevice             | Sortant   | Annonce la volonté d'exporter un compte.     |
| confirmAddDevice      | Sortant   | Confirme l'adresse de l'appareil exportant.  |
| cancelAddDevice       | Sortant   | Annuler l'opération.                         |
| addDeviceStateChanged | Entrant   | Indique le nouvel état et porte les détails. |