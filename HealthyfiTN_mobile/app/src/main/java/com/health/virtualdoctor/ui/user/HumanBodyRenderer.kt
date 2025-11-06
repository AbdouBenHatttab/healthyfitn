package com.health.virtualdoctor.ui.user

import android.content.Context
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class HumanBodyRenderer(private val context: Context) {
    private lateinit var webView: WebView

    fun getView(): View {
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webViewClient = WebViewClient()
        }
        return webView
    }

    fun loadModel() {
        val htmlContent = """
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #1a1a2e;
            overflow: hidden;
            width: 100vw;
            height: 100vh;
        }
        #container {
            width: 100%;
            height: 100%;
            position: relative;
        }
        #canvas3d {
            width: 100%;
            height: 100%;
            display: block;
        }
        #info {
            position: absolute;
            top: 10px;
            left: 10px;
            background: rgba(255, 255, 255, 0.95);
            padding: 12px;
            border-radius: 10px;
            font-size: 11px;
            max-width: 200px;
            display: none;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        }
        #info h3 {
            margin: 0 0 8px 0;
            font-size: 13px;
            color: #667eea;
        }
    </style>
</head>
<body>
<div id="container">
    <canvas id="canvas3d"></canvas>
    <div id="info">
        <h3 id="infoTitle">Zone</h3>
        <div id="infoContent"></div>
    </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
<script>
    let scene, camera, renderer, humanBody;
    let heartMesh, brainMesh, lungsMesh, musclesMesh, stomachMesh;
    let mouseDown = false, mouseX = 0, mouseY = 0;
    let targetRotationX = 0, targetRotationY = 0;
    let currentRotationX = 0, currentRotationY = 0;
    let raycaster, mouse;

    const healthData = {
        heartRate: 70,
        oxygen: 98,
        temperature: 37,
        stress: 20,
        muscle: 45
    };

    function init() {
        scene = new THREE.Scene();
        scene.background = new THREE.Color(0x1a1a2e);

        camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 1000);
        camera.position.set(0, 1, 5);
        camera.lookAt(0, 1, 0);

        renderer = new THREE.WebGLRenderer({
            canvas: document.getElementById('canvas3d'),
            antialias: true
        });
        renderer.setSize(window.innerWidth, window.innerHeight);
        renderer.shadowMap.enabled = true;

        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
        scene.add(ambientLight);

        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(5, 10, 5);
        directionalLight.castShadow = true;
        scene.add(directionalLight);

        const pointLight1 = new THREE.PointLight(0x667eea, 0.5);
        pointLight1.position.set(-3, 2, 2);
        scene.add(pointLight1);

        const pointLight2 = new THREE.PointLight(0x764ba2, 0.5);
        pointLight2.position.set(3, 2, 2);
        scene.add(pointLight2);

        raycaster = new THREE.Raycaster();
        mouse = new THREE.Vector2();

        createHumanBody();
        setupEventListeners();
        updateHealthIndicators();
        animate();
    }

    function createHumanBody() {
        humanBody = new THREE.Group();

        // Head
        const headGeometry = new THREE.SphereGeometry(0.25, 32, 32);
        const headMaterial = new THREE.MeshPhongMaterial({
            color: 0xffdbac,
            shininess: 30
        });
        const head = new THREE.Mesh(headGeometry, headMaterial);
        head.position.y = 2;
        head.castShadow = true;
        head.userData = { name: 'head', type: 'head' };
        humanBody.add(head);

        // Brain
        const brainGeometry = new THREE.SphereGeometry(0.18, 32, 32);
        const brainMaterial = new THREE.MeshPhongMaterial({
            color: 0xff69b4,
            emissive: 0xff1493,
            emissiveIntensity: 0.3,
            transparent: true,
            opacity: 0.8
        });
        brainMesh = new THREE.Mesh(brainGeometry, brainMaterial);
        brainMesh.position.copy(head.position);
        brainMesh.userData = { name: 'brain', type: 'organ' };
        humanBody.add(brainMesh);

        // Neck
        const neckGeometry = new THREE.CylinderGeometry(0.12, 0.12, 0.3, 16);
        const neckMaterial = new THREE.MeshPhongMaterial({ color: 0xffdbac });
        const neck = new THREE.Mesh(neckGeometry, neckMaterial);
        neck.position.y = 1.7;
        neck.castShadow = true;
        humanBody.add(neck);

        // Torso
        const torsoGeometry = new THREE.CylinderGeometry(0.35, 0.3, 0.9, 16);
        const torsoMaterial = new THREE.MeshPhongMaterial({ color: 0xffdbac });
        const torso = new THREE.Mesh(torsoGeometry, torsoMaterial);
        torso.position.y = 1.1;
        torso.castShadow = true;
        torso.userData = { name: 'torso', type: 'torso' };
        humanBody.add(torso);

        // Heart
        const heartGeometry = new THREE.SphereGeometry(0.12, 32, 32);
        const heartMaterial = new THREE.MeshPhongMaterial({
            color: 0xff0000,
            emissive: 0xff0000,
            emissiveIntensity: 0.5,
            transparent: true,
            opacity: 0.9
        });
        heartMesh = new THREE.Mesh(heartGeometry, heartMaterial);
        heartMesh.position.set(-0.1, 1.2, 0.35);
        heartMesh.userData = { name: 'heart', type: 'organ' };
        humanBody.add(heartMesh);

        // Lungs
        const lungGeometry = new THREE.SphereGeometry(0.15, 16, 16);
        const lungMaterial = new THREE.MeshPhongMaterial({
            color: 0x87ceeb,
            emissive: 0x4682b4,
            emissiveIntensity: 0.2,
            transparent: true,
            opacity: 0.7
        });

        lungsMesh = new THREE.Group();
        
        const leftLung = new THREE.Mesh(lungGeometry, lungMaterial);
        leftLung.position.set(0.2, 1.2, 0.25);
        leftLung.scale.set(0.8, 1.2, 0.6);
        leftLung.userData = { name: 'leftLung', type: 'organ' };
        lungsMesh.add(leftLung);

        const rightLung = new THREE.Mesh(lungGeometry, lungMaterial.clone());
        rightLung.position.set(-0.25, 1.2, 0.25);
        rightLung.scale.set(0.8, 1.2, 0.6);
        rightLung.userData = { name: 'rightLung', type: 'organ' };
        lungsMesh.add(rightLung);
        
        humanBody.add(lungsMesh);

        // Stomach
        const stomachGeometry = new THREE.SphereGeometry(0.15, 16, 16);
        const stomachMaterial = new THREE.MeshPhongMaterial({
            color: 0xffa500,
            emissive: 0xff8c00,
            emissiveIntensity: 0.2,
            transparent: true,
            opacity: 0.7
        });
        stomachMesh = new THREE.Mesh(stomachGeometry, stomachMaterial);
        stomachMesh.position.set(0, 0.85, 0.25);
        stomachMesh.scale.set(1, 0.8, 0.8);
        stomachMesh.userData = { name: 'stomach', type: 'organ' };
        humanBody.add(stomachMesh);

        // Arms
        const armGeometry = new THREE.CylinderGeometry(0.08, 0.08, 0.8, 16);
        const armMaterial = new THREE.MeshPhongMaterial({ color: 0xffdbac });

        const leftArm = new THREE.Mesh(armGeometry, armMaterial);
        leftArm.position.set(0.45, 1.1, 0);
        leftArm.rotation.z = 0.3;
        leftArm.castShadow = true;
        humanBody.add(leftArm);

        const rightArm = new THREE.Mesh(armGeometry, armMaterial);
        rightArm.position.set(-0.45, 1.1, 0);
        rightArm.rotation.z = -0.3;
        rightArm.castShadow = true;
        humanBody.add(rightArm);

        // Muscles
        const muscleGeometry = new THREE.SphereGeometry(0.1, 16, 16);
        const muscleMaterial = new THREE.MeshPhongMaterial({
            color: 0xff6b6b,
            emissive: 0xff0000,
            emissiveIntensity: 0.2
        });

        musclesMesh = new THREE.Group();
        
        const leftMuscle = new THREE.Mesh(muscleGeometry, muscleMaterial);
        leftMuscle.position.set(0.45, 1.3, 0);
        leftMuscle.scale.set(1.2, 0.8, 0.8);
        leftMuscle.userData = { name: 'leftMuscle', type: 'muscle' };
        musclesMesh.add(leftMuscle);

        const rightMuscle = new THREE.Mesh(muscleGeometry, muscleMaterial.clone());
        rightMuscle.position.set(-0.45, 1.3, 0);
        rightMuscle.scale.set(1.2, 0.8, 0.8);
        rightMuscle.userData = { name: 'rightMuscle', type: 'muscle' };
        musclesMesh.add(rightMuscle);
        
        humanBody.add(musclesMesh);

        // Pelvis
        const pelvisGeometry = new THREE.CylinderGeometry(0.3, 0.35, 0.3, 16);
        const pelvisMaterial = new THREE.MeshPhongMaterial({ color: 0xffdbac });
        const pelvis = new THREE.Mesh(pelvisGeometry, pelvisMaterial);
        pelvis.position.y = 0.5;
        pelvis.castShadow = true;
        humanBody.add(pelvis);

        // Legs
        const legGeometry = new THREE.CylinderGeometry(0.1, 0.09, 1, 16);
        const legMaterial = new THREE.MeshPhongMaterial({ color: 0xffdbac });

        const leftLeg = new THREE.Mesh(legGeometry, legMaterial);
        leftLeg.position.set(0.15, 0, 0);
        leftLeg.castShadow = true;
        humanBody.add(leftLeg);

        const rightLeg = new THREE.Mesh(legGeometry, legMaterial);
        rightLeg.position.set(-0.15, 0, 0);
        rightLeg.castShadow = true;
        humanBody.add(rightLeg);

        humanBody.position.y = 0.5;
        scene.add(humanBody);
    }

    function updateHealthIndicators() {
        // Heart Rate
        const hr = healthData.heartRate;
        let heartColor;
        if (hr < 60 || hr > 100) {
            heartColor = 0xff0000;
        } else if (hr > 90) {
            heartColor = 0xffa500;
        } else {
            heartColor = 0xff69b4;
        }
        heartMesh.material.color.setHex(heartColor);
        heartMesh.material.emissive.setHex(heartColor);

        // Oxygen
        const o2 = healthData.oxygen;
        let lungColor;
        if (o2 < 90) {
            lungColor = 0xff0000;
        } else if (o2 < 95) {
            lungColor = 0xffa500;
        } else {
            lungColor = 0x87ceeb;
        }
        lungsMesh.traverse((child) => {
            if (child.isMesh) {
                child.material.color.setHex(lungColor);
                child.material.emissive.setHex(lungColor);
            }
        });

        // Temperature
        const temp = healthData.temperature;
        let brainColor;
        if (temp > 38 || temp < 36) {
            brainColor = 0xff0000;
        } else if (temp > 37.5) {
            brainColor = 0xffa500;
        } else {
            brainColor = 0xff69b4;
        }
        brainMesh.material.color.setHex(brainColor);
        brainMesh.material.emissive.setHex(brainColor);

        // Muscles
        const muscle = healthData.muscle;
        let muscleColor = new THREE.Color();
        muscleColor.setHSL(0, muscle / 100, 0.5);
        musclesMesh.traverse((child) => {
            if (child.isMesh) {
                child.material.color.copy(muscleColor);
                child.material.emissive.copy(muscleColor);
                child.material.emissiveIntensity = muscle / 200;
            }
        });
    }

    function setupEventListeners() {
        renderer.domElement.addEventListener('mousedown', onMouseDown);
        renderer.domElement.addEventListener('mousemove', onMouseMove);
        renderer.domElement.addEventListener('mouseup', onMouseUp);
        renderer.domElement.addEventListener('wheel', onWheel);
        renderer.domElement.addEventListener('click', onBodyClick);
        window.addEventListener('resize', onWindowResize);
    }

    function onMouseDown(event) {
        mouseDown = true;
        mouseX = event.clientX;
        mouseY = event.clientY;
    }

    function onMouseMove(event) {
        if (!mouseDown) return;
        const deltaX = event.clientX - mouseX;
        const deltaY = event.clientY - mouseY;
        targetRotationY += deltaX * 0.01;
        targetRotationX += deltaY * 0.01;
        targetRotationX = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, targetRotationX));
        mouseX = event.clientX;
        mouseY = event.clientY;
    }

    function onMouseUp() {
        mouseDown = false;
    }

    function onWheel(event) {
        event.preventDefault();
        camera.position.z += event.deltaY * 0.005;
        camera.position.z = Math.max(2, Math.min(10, camera.position.z));
    }

    function onBodyClick(event) {
        mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
        mouse.y = -(event.clientY / window.innerHeight) * 2 + 1;
        raycaster.setFromCamera(mouse, camera);
        
        // Récupérer tous les objets incluant les groupes et leurs enfants
        const allObjects = [];
        humanBody.traverse((child) => {
            if (child.isMesh) {
                allObjects.push(child);
            }
        });
        
        const intersects = raycaster.intersectObjects(allObjects, false);
        if (intersects.length > 0) {
            showBodyPartInfo(intersects[0].object);
        }
    }

    function showBodyPartInfo(object) {
        const infoDiv = document.getElementById('info');
        const infoTitle = document.getElementById('infoTitle');
        const infoContent = document.getElementById('infoContent');
        const userData = object.userData;
        let title = '';
        let content = '';

        switch(userData.name) {
            case 'heart':
                title = '❤️ Cœur';
                content = 'Fréquence: ' + healthData.heartRate + ' BPM<br>État: ' + 
                    (healthData.heartRate > 100 ? 'Élevé' : healthData.heartRate < 60 ? 'Bas' : 'Normal');
                break;
            case 'brain':
                title = '🧠 Cerveau';
                content = 'Température: ' + healthData.temperature + '°C<br>Stress: ' + healthData.stress + '/100';
                break;
            case 'leftLung':
            case 'rightLung':
                title = '🫁 Poumons';
                content = 'SpO₂: ' + healthData.oxygen + '%<br>État: ' + 
                    (healthData.oxygen < 95 ? 'Attention' : 'Excellent');
                break;
            case 'stomach':
                title = '🍽️ Estomac';
                content = 'Hydratation en cours<br>Digestion: Normale';
                break;
            case 'leftMuscle':
            case 'rightMuscle':
                title = '💪 Muscles';
                content = 'Activité: ' + healthData.muscle + '%<br>Fatigue: ' + (100 - healthData.muscle) + '%';
                break;
            default:
                title = '👤 Corps';
                content = 'Cliquez sur les organes pour détails';
        }

        infoTitle.textContent = title;
        infoContent.innerHTML = content;
        infoDiv.style.display = 'block';

        setTimeout(() => {
            infoDiv.style.display = 'none';
        }, 3000);
    }

    function onWindowResize() {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    }

    function animate() {
        requestAnimationFrame(animate);
        currentRotationX += (targetRotationX - currentRotationX) * 0.1;
        currentRotationY += (targetRotationY - currentRotationY) * 0.1;
        humanBody.rotation.y = currentRotationY;
        humanBody.rotation.x = currentRotationX;

        const pulse = Math.sin(Date.now() * 0.008) * 0.05 + 1;
        heartMesh.scale.set(pulse, pulse, pulse);

        const breath = Math.sin(Date.now() * 0.002) * 0.03 + 1;
        lungsMesh.traverse((child) => {
            if (child.isMesh) {
                child.scale.y = 1.2 * breath;
            }
        });

        renderer.render(scene, camera);
    }

    window.updateHealthData = function(data) {
        healthData.heartRate = data.heartRate || 70;
        healthData.oxygen = data.oxygen || 98;
        healthData.temperature = data.temperature || 37;
        healthData.stress = data.stress || 20;
        healthData.muscle = data.muscle || 45;
        updateHealthIndicators();
    };

    init();
</script>
</body>
</html>
        """.trimIndent()

        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
    }

    fun updateHealthData(dayJson: JSONObject) {
        try {
            val avgHeartRate = dayJson.optInt("avgHeartRate", 70)
            val totalSteps = dayJson.optLong("totalSteps", 0)
            val sleepHours = dayJson.optString("totalSleepHours", "7.0").toDoubleOrNull() ?: 7.0

            // SpO2
            val oxygenArray = dayJson.optJSONArray("oxygenSaturation")
            val oxygen = if (oxygenArray != null && oxygenArray.length() > 0) {
                oxygenArray.getJSONObject(oxygenArray.length() - 1).optDouble("percentage", 98.0).toInt()
            } else {
                98
            }

            // Temperature
            val tempArray = dayJson.optJSONArray("bodyTemperature")
            val temperature = if (tempArray != null && tempArray.length() > 0) {
                tempArray.getJSONObject(tempArray.length() - 1).optDouble("temperature", 37.0)
            } else {
                37.0
            }

            // Stress calculation
            val stressScore = dayJson.optInt("stressScore", 20)

            // Muscle activity based on steps
            val muscleActivity = when {
                totalSteps > 10000 -> 80
                totalSteps > 7000 -> 60
                totalSteps > 5000 -> 45
                totalSteps > 2000 -> 30
                else -> 15
            }

            val javascript = """
                window.updateHealthData({
                    heartRate: $avgHeartRate,
                    oxygen: $oxygen,
                    temperature: $temperature,
                    stress: $stressScore,
                    muscle: $muscleActivity
                });
            """.trimIndent()

            webView.evaluateJavascript(javascript, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}