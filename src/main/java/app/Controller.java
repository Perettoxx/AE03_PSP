package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

	static File directoriPelis = new File("./pelis");
	static String[] llistaPelis = directoriPelis.list(new FiltreExtensio(".txt"));

	/**
	 * Funció GET que retorna la informació de les pelis depenent del parametre que se ha donat.
	 * @param parametre ID de la peli a obtenir informació. 'all' per a mostrar totes les pelis.
	 * @return ResponseEntity on es troba la informació de la peli  o notificació en cas de que hi haja un error al ID.
	 */
	@GetMapping("APIpelis/t")
	ResponseEntity<String> informacioPelis(@RequestParam(value = "id") String parametre) {

		llistaPelis = directoriPelis.list(new FiltreExtensio(".txt"));
		String info = "";

		if (parametre.equals("all")) {
			JSONObject infoJSON = new JSONObject();
			JSONArray titols = new JSONArray();

			for (int i = 0; i < llistaPelis.length; i++) {
				try {
					FileReader fr = new FileReader("./pelis/" + llistaPelis[i]);
					BufferedReader br = new BufferedReader(fr);

					JSONObject peliculaJSON = new JSONObject();

					String id = llistaPelis[i].split("\\.")[0];
					peliculaJSON.put("id", id);

					String titol = br.readLine().split(":")[1].trim();
					peliculaJSON.put("titol", titol);

					titols.put(peliculaJSON);

					fr.close();
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			infoJSON.put("titols", titols);
			info = infoJSON.toString();
			System.out.println(info);
		} else {
			boolean exists = false;

			for (int i = 0; i < llistaPelis.length; i++) {

				String id = llistaPelis[i].split("\\.")[0];
				if (id.equals(parametre)) {
					exists = true;
				}
			}

			if (!exists) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).header("Content-Length", "0")
						.body("No s'ha trobat res");
			} else {

				try {

					FileReader fr = new FileReader("./pelis/" + parametre + ".txt");

					BufferedReader br = new BufferedReader(fr);

					JSONObject peliculaJSON = new JSONObject();

					peliculaJSON.put("id", parametre);

					String titol = br.readLine().split(":")[1].trim();
					peliculaJSON.put("titol", titol);

					JSONArray resenyaJSON = new JSONArray();
					String linia;

					while ((linia = br.readLine()) != null) {
						String resenya = linia.trim();
						resenyaJSON.put(resenya);
					}

					br.close();
					fr.close();

					peliculaJSON.put("ressenyes", resenyaJSON);
					info = peliculaJSON.toString();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return ResponseEntity.ok(info);

	}

	/**
	 * Funció POST que permeteix la inserció de una resenya en un peli.
	 * @param stringJSON El JSON amb les dades de la resenya (usuari, ID de la peli i resenya).
	 * @return ResponseEntity on es troba la informació de la resposta de la API. No notifica res en cas de que tot siga correcte. Notifica errors de autorització de usuari i de error a la petició.
	 */
	@PostMapping("APIpelis/novaRessenya")
	ResponseEntity<String> novaResenya(@RequestBody String stringJSON) {
		JSONObject body = new JSONObject(stringJSON);

		try {
			boolean exists = false;
			
			String usuari = (String) body.get("usuari");
			String id = (String) body.get("id");
			String resenya = (String) body.get("ressenya");


			for (int i = 0; i < llistaPelis.length; i++) {

				String idReal = llistaPelis[i].split("\\.")[0];
				if (idReal.equals(id)) {
					exists = true;
				}
			}

			if (exists) {
				if (autorizarUsuari(usuari)) {
					FileWriter fw = new FileWriter("./pelis/" + id + ".txt", true);
					fw.write("\n" + usuari + ": " + resenya);
					fw.close();
					return ResponseEntity.noContent().header("Content-Length", "0").build();
				} else {
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Content-Length", "0").build();
				}
			} else {
				return ResponseEntity.status(HttpStatus.CONFLICT).header("Content-Length", "0").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

	}

	/**
	 * Funció POST que permeteix la inserció de una nova peli.
	 * @param stringJSON El JSON amb les dades de la peli (titol, usuari).
	 * @return ResponseEntity on es troba la informació de la resposta de la API. No notifica res en cas de que tot siga correcte. Notifica errors de autorització de usuari i de error a la petició.
	 */
	@PostMapping("APIpelis/novaPeli")
	ResponseEntity<String> novaPeli(@RequestBody String stringJSON) {
		JSONObject body = new JSONObject(stringJSON);
		try {
			String titol = (String) body.get("titol");
			String usuari = (String) body.get("usuari");
			if (autorizarUsuari(usuari)) {
				llistaPelis = directoriPelis.list(new FiltreExtensio(".txt"));
				int id = llistaPelis.length + 1;
				File arxiu = new File("./pelis/" + id + ".txt");
				if (!arxiu.exists()) {
					if (arxiu.createNewFile()) {
						FileWriter fw = new FileWriter("./pelis/" + id + ".txt", true);
						fw.write("Titol: " + titol);
						fw.close();
					}
				}
				return ResponseEntity.noContent().header("Content-Length", "0").build();
			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Content-Length", "0").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

	}

	/**
	 * Funció POST que permeteix la inserció de un nou usuari.
	 * @param stringJSON El JSON amb el nom de l'usuari.
	 * @return ResponseEntity on es troba la informació de la resposta de la API. No notifica res en cas de que tot siga correcte (també autoritza al usuari). Notifica errors en cas de trobar que l'usuari ja está i de error a la petició.
	 */
	@PostMapping("APIpelis/nouUsuari")
	ResponseEntity<String> nouUsuari(@RequestBody String stringJSON) {
		JSONObject body = new JSONObject(stringJSON);
		try {
			String usuari = (String) body.get("usuari");
			if (!autorizarUsuari(usuari)) {
				FileWriter fw = new FileWriter("./autoritzats/autorizados.txt", true);
				fw.write("\nUsuari:" + usuari);
				fw.close();
				return ResponseEntity.noContent().header("Content-Length", "0").build();
			} else {
				return ResponseEntity.status(HttpStatus.CONFLICT).header("Content-Length", "0").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

	}

	/**
	 * Funció interna que busca al fitxer de autorizados.txt l'usuari que es pasa per parámetre.
	 * @param nomUsuari String de l'usuari a buscar.
	 * @return boolean Retorna true en cas de trobarse al fitxer o false en cas contrari.
	 */
	@SuppressWarnings("resource")
	private boolean autorizarUsuari(String nomUsuari) {

		try {
			FileReader fr = new FileReader("./autoritzats/autorizados.txt");
			BufferedReader br = new BufferedReader(fr);
			String linea = br.readLine();
			while ((linea = br.readLine()) != null) {
				if (!linea.isBlank()) {
					String[] nomPartit = linea.split(":");
					if (nomUsuari.equals(nomPartit[1]))
						return true;
				}
			}
			br.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
