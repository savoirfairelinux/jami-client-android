#!/bin/bash

# Définir le répertoire source sur l'appareil Android (émulateur)
screenshotDir="/sdcard/Download/screenshots"  # Chemin du répertoire sur l'émulateur

# Définir le répertoire de destination sur ton système local
localDir="/jami-client-android/ci/spoon-output/screenshots"  # Dossier pour stocker les captures d'écran localement

# Créer le répertoire local s'il n'existe pas
mkdir -p "$localDir"

# Télécharger le contenu du répertoire de l'appareil vers le répertoire local
adb pull "$screenshotDir" "$localDir"

# Vérifier si le téléchargement a réussi
if [ $? -eq 0 ]; then
    echo "Le contenu du répertoire '$screenshotDir' a été téléchargé dans '$localDir'."
else
    echo "Erreur lors du téléchargement du répertoire."
fi

chown -R jenkins:jenkins "$localDir"
