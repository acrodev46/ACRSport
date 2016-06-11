		ACRSport

Application créé sous android Studio 2.0 SDK version 23.0.3.
Base de donnée créé à l'aide d'Azure mobile App.

L'application ACRSport sert à noter les heures de passage des equipes d'un raid pour les enregistrer dans la base de donnée azure.


--------------- Service Azure ---------------

La connection au service Azure se fait grace à la classe MobileServiceClient qui se connecte a l'adresse : https://acrsport.azurewebsites.net

L'autentification se fait avec la fonction autenticate qui utilise le service Windows Azure Active Directory pour créer un jeton de connection.
Grace à ce jeton la connection n'a besoin d'être etabli seulement lors de la premiere ouverture du programme.

L'application étant crée pour fonctionner hors connection, un local Store est créé pour sauvegarder les données en attendant de retrouver une connection stable.
La fonction synchroniser permet d'envoyer ses modifications sur la base de donnée et de récuperer les modifications venant d'autres apareils.

Lors de l'abandon d'une équipe, l'application se connecte au hub de notification ACRSportHub grace a la chaine de connection : Endpoint=sb://acrsport.servicebus.windows.net/;SharedAccessKeyName=DefaultFullSharedAccessSignature;SharedAccessKey=XWB+tpV78KR7UWmxd65CBhMD22JKii8RwSHNzV60XBA=
et crée une notification grace a la methode sendNotification de la classe MyHandler (fille de NotificationHandler).
Les autres apareils vont recevoir la notification a l'aide de la méthode onReceive.



--------------- Android ---------------

Une fois les tables récupérées, elle sont affichées lors de Quatres activitées differentes :

- Main Activity
	Affiche la liste de toutes les équipes.
	Appuis sur suivi temps : déclanche Choix Position.
	Appuis sur une equipe : déclanche Temps Equipe

- Choix Position
	Affiche la liste des points de passage  pour en choisir un.
	Déclanche Suivi Temps

- Suivi Temps
	Affiche la liste des temps des équipes passées au point choisis.
	Permet de rentrer le numero d'une équipe pour enregistrer son temps.
	Enregistre au meme moment les coordonnées GPS et l'id de l'utilisateur.
	Appuis sur un temps : Supprime le temps sélectionné.

- Temps Equipe
	Affiche les temps de l'équipe
