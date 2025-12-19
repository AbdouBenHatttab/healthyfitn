"""
Script de test pour l'authentification Keycloak
Utilise le auth-service pour obtenir un token, puis teste nutrition-service
"""

import requests
import json
import sys

# URLs des services
AUTH_SERVICE_URL = "http://localhost:8082"
NUTRITION_SERVICE_URL = "http://localhost:8086"

def test_keycloak_integration():
    """Test complet de l'intégration Keycloak"""

    print("=" * 60)
    print("🧪 TEST INTÉGRATION KEYCLOAK - NUTRITION SERVICE")
    print("=" * 60)

    # ========================================
    # ÉTAPE 1: Login via auth-service
    # ========================================
    print("\n📝 ÉTAPE 1: Login via auth-service...")

    login_data = {
        "email": "rayenbenzid1@gmail.com",  # Remplacer par un utilisateur existant
        "password": "Rayen123!"
    }

    try:
        response = requests.post(
            f"{AUTH_SERVICE_URL}/api/v1/auth/login",
            json=login_data,
            timeout=10
        )

        if response.status_code != 200:
            print(f"❌ Login échoué: {response.status_code}")
            print(f"Response: {response.text}")
            return False

        auth_response = response.json()
        access_token = auth_response['accessToken']
        user = auth_response['user']

        print(f"✅ Login réussi!")
        print(f"   Email: {user['email']}")
        print(f"   Roles: {user['roles']}")
        print(f"   Token (preview): {access_token[:50]}...")

    except Exception as e:
        print(f"❌ Erreur login: {str(e)}")
        return False

    # ========================================
    # ÉTAPE 2: Test endpoint protégé nutrition-service
    # ========================================
    print("\n🔐 ÉTAPE 2: Test endpoint protégé nutrition-service...")

    headers = {
        "Authorization": f"Bearer {access_token}"
    }

    # Test 1: Get nutrition history
    print("\n  Test 1: GET /api/v1/nutrition/history")
    try:
        response = requests.get(
            f"{NUTRITION_SERVICE_URL}/api/v1/nutrition/history",
            headers=headers,
            timeout=10
        )

        if response.status_code == 200:
            print(f"  ✅ Succès! Status: {response.status_code}")
            data = response.json()
            print(f"     Analyses count: {data['data']['count']}")
        else:
            print(f"  ❌ Échec: {response.status_code}")
            print(f"     Response: {response.text}")
            return False

    except Exception as e:
        print(f"  ❌ Erreur: {str(e)}")
        return False

    # Test 2: Get statistics
    print("\n  Test 2: GET /api/v1/nutrition/statistics")
    try:
        response = requests.get(
            f"{NUTRITION_SERVICE_URL}/api/v1/nutrition/statistics",
            headers=headers,
            timeout=10
        )

        if response.status_code == 200:
            print(f"  ✅ Succès! Status: {response.status_code}")
            data = response.json()
            print(f"     Total analyses: {data['data']['total_analyses']}")
        else:
            print(f"  ❌ Échec: {response.status_code}")
            print(f"     Response: {response.text}")
            return False

    except Exception as e:
        print(f"  ❌ Erreur: {str(e)}")
        return False

    # Test 3: Model status
    print("\n  Test 3: GET /api/v1/nutrition/model/status")
    try:
        response = requests.get(
            f"{NUTRITION_SERVICE_URL}/api/v1/nutrition/model/status",
            headers=headers,
            timeout=10
        )

        if response.status_code == 200:
            print(f"  ✅ Succès! Status: {response.status_code}")
            data = response.json()
            print(f"     Model status: {data['data']['status']}")
        else:
            print(f"  ❌ Échec: {response.status_code}")
            print(f"     Response: {response.text}")
            return False

    except Exception as e:
        print(f"  ❌ Erreur: {str(e)}")
        return False

    # ========================================
    # ÉTAPE 3: Test sans token (devrait échouer)
    # ========================================
    print("\n🚫 ÉTAPE 3: Test sans token (doit échouer)...")

    try:
        response = requests.get(
            f"{NUTRITION_SERVICE_URL}/api/v1/nutrition/history",
            timeout=10
        )

        if response.status_code == 401:
            print(f"  ✅ Échec attendu! Status: {response.status_code}")
            print(f"     Message: {response.json()['message']}")
        else:
            print(f"  ❌ Devrait retourner 401, a retourné: {response.status_code}")
            return False

    except Exception as e:
        print(f"  ❌ Erreur: {str(e)}")
        return False

    # ========================================
    # RÉSULTAT FINAL
    # ========================================
    print("\n" + "=" * 60)
    print("✅ TOUS LES TESTS PASSÉS!")
    print("=" * 60)

    return True


if __name__ == "__main__":
    print("\n🚀 Démarrage des tests d'intégration Keycloak...\n")

    # Vérifier que les services sont démarrés
    print("🔍 Vérification des services...")

    # Check auth-service
    try:
        resp = requests.get(f"{AUTH_SERVICE_URL}/api/v1/auth/health", timeout=5)
        if resp.status_code == 200:
            print("✅ auth-service: UP")
        else:
            print(f"⚠️  auth-service: Status {resp.status_code}")
    except:
        print("❌ auth-service: DOWN (vérifier qu'il est démarré)")
        sys.exit(1)

    # Check nutrition-service
    try:
        resp = requests.get(f"{NUTRITION_SERVICE_URL}/health", timeout=5)
        if resp.status_code == 200:
            print("✅ nutrition-service: UP")
        else:
            print(f"⚠️  nutrition-service: Status {resp.status_code}")
    except:
        print("❌ nutrition-service: DOWN (vérifier qu'il est démarré)")
        sys.exit(1)

    print()

    # Lancer les tests
    success = test_keycloak_integration()

    if not success:
        print("\n❌ TESTS ÉCHOUÉS")
        sys.exit(1)

    sys.exit(0)