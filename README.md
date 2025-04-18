# Liste de Produits - Application Android

Une application Android pour gérer des listes de produits qui se connecte à un backend API Python.

## Fonctionnalités

- **Authentification**: Connexion utilisateur sécurisée
- **Gestion des listes**: Création, visualisation et copie de listes de produits
- **Gestion des produits**: Ajout, modification, suppression et recherche de produits
- **Interface utilisateur intuitive**: Navigation fluide et design moderne
- **Synchronisation des données**: Toutes les données sont synchronisées avec le serveur backend

## Architecture et Technologies

- **Frontend**: Android (Java)
- **Communication réseau**: API REST
- **Gestion des sessions**: JWT (JSON Web Token)
- **Interface utilisateur**: RecyclerViews, AlertDialogs, MaterialDesign

## Prérequis

- Android Studio Meerkat (2024.3.1) ou plus récent
- Niveau d'API Android minimum: 21 (Android 5.0)
- JDK 17 ou plus récent
- Un appareil Android physique ou émulateur

## Installation

1. Clonez le dépôt:
   ```bash
   git clone https://github.com/MorgunovE/A17TPListProduits.git
   ```

2. Ouvrez le projet dans Android Studio

3. Synchronisez le projet avec Gradle

4. Exécutez l'application sur un émulateur ou un appareil physique

## Configuration de l'API Backend

**Important**: Cette application nécessite un serveur backend pour fonctionner.

1. Installez et démarrez d'abord le projet Python backend:
   ```bash
   git clone https://github.com/MorgunovE/a15tpproductlist.git
   ```

2. Suivez les instructions dans le README du projet Python pour démarrer le serveur API:
    - Option 1: Démarrage avec Docker
    - Option 2: Démarrage local

3. Par défaut, l'application Android est configurée pour se connecter à `http://127.0.0.1:5000/`

4. Si votre API est hébergée sur une autre adresse:
    - Modifiez l'URL dans `app/build.gradle.kts`
    - Ajustez la configuration réseau dans `app/src/main/res/xml/network_security_config.xml`

## Utilisation

1. Lancez l'application

2. Connectez-vous avec un compte existant ou créez-en un nouveau
    - Utilisateur par défaut: user
    - Mot de passe: password

3. Gestion des listes:
    - Visualisez vos listes existantes
    - Créez une nouvelle liste avec le bouton +
    - Appuyez sur une liste pour voir ses détails
    - Utilisez le bouton "Copier" pour dupliquer une liste

4. Gestion des produits:
    - Dans les détails d'une liste, ajoutez des produits avec le bouton +
    - Recherchez des produits existants
    - Créez de nouveaux produits
    - Modifiez la quantité d'un produit en appuyant dessus
    - Supprimez ou modifiez un produit en utilisant les boutons associés

## Données par défaut

L'application utilisera les données créées par le script d'initialisation du backend:

- **Utilisateur**: user / password
- **Produits**: Pomme, Lait (et autres que vous avez créés)
- **Listes**: Créez vos propres listes à partir de l'application

## Remarque importante

Cette application est conçue pour fonctionner avec le backend Python spécifique. Assurez-vous que le serveur est en cours d'exécution avant d'utiliser l'application.