document.addEventListener('DOMContentLoaded', function() {
    // Referencias a los tabs
    const infoTab = document.getElementById('info-tab');
    const locationTab = document.getElementById('location-tab');
    const itineraryTab = document.getElementById('itinerary-tab');
    
    // Referencias a los botones de navegación
    const toLocationBtn = document.getElementById('to-location-tab');
    const backToInfoBtn = document.getElementById('back-to-info-tab');
    const toItineraryBtn = document.getElementById('to-itinerary-tab');
    const backToLocationBtn = document.getElementById('back-to-location-tab');
    
    // Estado de validación de las pestañas
    let tabValidation = {
        info: false,
        location: false,
        itinerary: false
    };
    
    // Detectar si estamos en modo edición
    const isEditMode = window.location.pathname.includes('/editar/');
    
    // Inicializar estado de las pestañas
    if (!isEditMode) {
        initializeTabStates();
    } else {
        // En modo edición, habilitar todas las pestañas
        tabValidation.info = true;
        tabValidation.location = true;
        tabValidation.itinerary = true;
        updateTabIndicators();
    }
    
    function initializeTabStates() {
        // Deshabilitar pestañas no completadas
        locationTab.classList.add('disabled');
        itineraryTab.classList.add('disabled');
        locationTab.style.pointerEvents = 'none';
        itineraryTab.style.pointerEvents = 'none';
        
        // Añadir indicadores visuales
        updateTabIndicators();
    }
    
    function updateTabIndicators() {
        // Actualizar indicadores en las pestañas
        updateTabIndicator('info', tabValidation.info);
        updateTabIndicator('location', tabValidation.location);
        updateTabIndicator('itinerary', tabValidation.itinerary);
    }
    
    function updateTabIndicator(tabName, isValid) {
        const tab = document.getElementById(tabName + '-tab');
        const icon = tab.querySelector('.tab-icon');
        
        // Remover iconos existentes
        if (icon) {
            icon.remove();
        }
        
        // Añadir nuevo icono
        const newIcon = document.createElement('i');
        newIcon.className = 'tab-icon ms-2';
        
        if (isValid) {
            newIcon.className += ' bi bi-check-circle text-success';
        } else {
            newIcon.className += ' bi bi-circle text-muted';
        }
        
        tab.appendChild(newIcon);
    }
    
    function enableTab(tabElement) {
        tabElement.classList.remove('disabled');
        tabElement.style.pointerEvents = 'auto';
    }
    
    function disableTab(tabElement) {
        tabElement.classList.add('disabled');
        tabElement.style.pointerEvents = 'none';
    }
    
    // Prevenir navegación directa a pestañas no validadas (solo en modo creación)
    if (!isEditMode) {
        locationTab.addEventListener('click', function(e) {
            if (!tabValidation.info) {
                e.preventDefault();
                e.stopPropagation();
                showNotification('warning', 'Pestaña bloqueada', 'Primero debes completar la información básica');
                return false;
            }
        });
        
        itineraryTab.addEventListener('click', function(e) {
            if (!tabValidation.info || !tabValidation.location) {
                e.preventDefault();
                e.stopPropagation();
                const message = !tabValidation.info ? 
                    'Primero debes completar la información básica' : 
                    'Primero debes completar el punto de encuentro';
                showNotification('warning', 'Pestaña bloqueada', message);
                return false;
            }
        });
    }
    
    // Navegación entre tabs con validación mejorada
    if (toLocationBtn) {
        toLocationBtn.addEventListener('click', function() {
            if (isEditMode || validateInfoTab()) {
                tabValidation.info = true;
                enableTab(locationTab);
                updateTabIndicators();
                new bootstrap.Tab(locationTab).show();
            }
        });
    }
    
    if (backToInfoBtn) {
        backToInfoBtn.addEventListener('click', function() {
            new bootstrap.Tab(infoTab).show();
        });
    }
    
    if (toItineraryBtn) {
        toItineraryBtn.addEventListener('click', function() {
            if (isEditMode || validateLocationTab()) {
                tabValidation.location = true;
                enableTab(itineraryTab);
                updateTabIndicators();
                new bootstrap.Tab(itineraryTab).show();
                updateItinerary();
            }
        });
    }
    
    if (backToLocationBtn) {
        backToLocationBtn.addEventListener('click', function() {
            new bootstrap.Tab(locationTab).show();
        });
    }
    
    // Validación en tiempo real para revalidar pestañas (solo en modo creación)
    if (!isEditMode) {
        document.getElementById('nombreViaje').addEventListener('input', validateInfoTabRealTime);
        document.getElementById('destinoPrincipal').addEventListener('input', validateInfoTabRealTime);
        document.getElementById('fechaInicio').addEventListener('change', validateInfoTabRealTime);
        document.getElementById('fechaFin').addEventListener('change', validateInfoTabRealTime);
        document.getElementById('participantes').addEventListener('change', validateInfoTabRealTime);
        document.getElementById('descripcion').addEventListener('input', validateInfoTabRealTime);
        document.getElementById('puntoEncuentro').addEventListener('input', validateLocationTabRealTime);
        
        function validateInfoTabRealTime() {
            const isValid = validateInfoTab(true); // Validación silenciosa
            if (isValid !== tabValidation.info) {
                tabValidation.info = isValid;
                if (isValid) {
                    enableTab(locationTab);
                } else {
                    disableTab(locationTab);
                    disableTab(itineraryTab);
                    tabValidation.location = false;
                    tabValidation.itinerary = false;
                }
                updateTabIndicators();
            }
        }
        
        function validateLocationTabRealTime() {
            const isValid = validateLocationTab(true); // Validación silenciosa
            if (isValid !== tabValidation.location) {
                tabValidation.location = isValid;
                if (isValid && tabValidation.info) {
                    enableTab(itineraryTab);
                } else {
                    disableTab(itineraryTab);
                    tabValidation.itinerary = false;
                }
                updateTabIndicators();
            }
        }
    }

    // Manejo de etiquetas
    const etiquetasInput = document.getElementById('etiquetas');
    const etiquetasHidden = document.getElementById('etiquetasHidden');
    const tagsContainer = document.getElementById('tags-container');
    
    let tags = [];
    
    // Inicializar etiquetas existentes en modo edición
    if (isEditMode && etiquetasHidden && etiquetasHidden.value) {
        try {
            const etiquetasExistentes = JSON.parse(etiquetasHidden.value);
            if (Array.isArray(etiquetasExistentes)) {
                tags = etiquetasExistentes;
                updateTagsDisplay();
            }
        } catch (e) {
            console.error('Error al cargar etiquetas existentes:', e);
        }
    }
    
    if (etiquetasInput) {
        etiquetasInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                const tag = etiquetasInput.value.trim();
                if (tag) {
                    addTag(tag);
                }
            }
        });

        etiquetasInput.addEventListener('blur', function() {
            const tag = etiquetasInput.value.trim();
            if (tag) {
                addTag(tag);
            }
        });
    }

    function addTag(tag) {
        if (!tag || tag.trim() === '') {
            showNotification('warning', 'Etiqueta vacía', 'Por favor ingresa una etiqueta válida');
            return;
        }

        tag = tag.trim().toLowerCase();
        
        if (tags.includes(tag)) {
            showNotification('warning', 'Etiqueta duplicada', 'Esta etiqueta ya ha sido agregada');
            return;
        }

        if (tags.length >= 10) {
            showNotification('warning', 'Límite alcanzado', 'No puedes agregar más de 10 etiquetas');
            return;
        }

        if (tag.length > 20) {
            showNotification('warning', 'Etiqueta muy larga', 'Las etiquetas no pueden tener más de 20 caracteres');
            return;
        }

        tags.push(tag);
        updateTagsDisplay();
        updateHiddenField();
        etiquetasInput.value = '';
        
        // Revalidar pestaña después de agregar etiqueta (solo en modo creación)
        if (!isEditMode) {
            validateInfoTabRealTime();
        }
        
        showNotification('toast', `Etiqueta "${tag}" agregada`);
    }
    
    function removeTag(tag) {
        const index = tags.indexOf(tag);
        if (index !== -1) {
            tags.splice(index, 1);
            updateTagsDisplay();
            updateHiddenField();
            
            // Revalidar pestaña después de eliminar etiqueta (solo en modo creación)
            if (!isEditMode) {
                validateInfoTabRealTime();
            }
            
            showNotification('toast', `Etiqueta "${tag}" eliminada`);
        }
    }
    
    function updateTagsDisplay() {
        if (tagsContainer) {
            tagsContainer.innerHTML = '';
            tags.forEach(tag => {
                const tagElement = document.createElement('span');
                tagElement.className = 'badge bg-primary text-white me-1 mb-1';
                tagElement.innerHTML = `${tag} <i class="bi bi-x-circle ms-1" style="cursor: pointer;"></i>`;
                tagElement.querySelector('i').addEventListener('click', function() {
                    removeTag(tag);
                });
                tagsContainer.appendChild(tagElement);
            });
        }
    }

    function updateHiddenField() {
        if (etiquetasHidden) {
            etiquetasHidden.value = JSON.stringify(tags);
        }
    }    

    // Manejo del itinerario
    const fechaInicio = document.getElementById('fechaInicio');
    const fechaFin = document.getElementById('fechaFin');
    const itineraryContainer = document.getElementById('itinerary-container');
    const noDatesWarning = document.getElementById('no-dates-warning');
    const tripDaysBadge = document.getElementById('trip-days-badge');
    const itineraryAccordion = document.getElementById('itineraryAccordion');
    const diasItinerarioJson = document.getElementById('diasItinerarioJson');
    
    let itineraryData = [];
    
    // Inicializar itinerario existente en modo edición
    if (isEditMode && diasItinerarioJson && diasItinerarioJson.value) {
        try {
            const itinerarioExistente = JSON.parse(diasItinerarioJson.value);
            if (Array.isArray(itinerarioExistente)) {
                itineraryData = itinerarioExistente;
                updateItinerary();
            }
        } catch (e) {
            console.error('Error al cargar itinerario existente:', e);
        }
    }
    
    function updateItinerary() {
        if (fechaInicio && fechaFin && fechaInicio.value && fechaFin.value) {
            const start = new Date(fechaInicio.value);
            const end = new Date(fechaFin.value);

            if (start > end) {
                if (noDatesWarning) {
                    noDatesWarning.classList.remove('d-none');
                    noDatesWarning.innerHTML = '<p class="d-flex align-items-center mb-0"><i class="bi bi-exclamation-triangle me-2"></i>La fecha de inicio no puede ser posterior a la fecha de fin.</p>';
                }
                if (itineraryContainer) {
                    itineraryContainer.classList.add('d-none');
                }
                return;
            }
            
            const diffTime = Math.abs(end - start);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;

            if (tripDaysBadge) {
                tripDaysBadge.textContent = `${diffDays} días de viaje`;
            }
            
            if (noDatesWarning) {
                noDatesWarning.classList.add('d-none');
            }
            if (itineraryContainer) {
                itineraryContainer.classList.remove('d-none');
            }

            // Solo regenerar si no hay datos existentes o el número de días cambió
            if (itineraryData.length === 0 || itineraryData.length !== diffDays) {
                generateItineraryDays(diffDays, start);
            } else {
                // Solo recrear la UI con los datos existentes
                recreateItineraryUI(diffDays, start);
            }
        } else {
            if (noDatesWarning) {
                noDatesWarning.classList.remove('d-none');
            }
            if (itineraryContainer) {
                itineraryContainer.classList.add('d-none');
            }
        }
    }

    function generateItineraryDays(days, startDate) {
        if (itineraryAccordion) {
            itineraryAccordion.innerHTML = '';
        }
        
        // Solo regenerar datos si no existen
        if (itineraryData.length === 0) {
            itineraryData = [];
            for (let i = 0; i < days; i++) {
                const dayData = {
                    diaNumero: i + 1,
                    titulo: `Día ${i + 1}`,
                    descripcion: '',
                    puntoPartida: '',
                    puntoLlegada: '',
                    duracionEstimada: ''
                };
                itineraryData.push(dayData);
            }
        }
        
        recreateItineraryUI(days, startDate);
    }

    function recreateItineraryUI(days, startDate) {
        if (!itineraryAccordion) return;
        
        itineraryAccordion.innerHTML = '';
        
        for (let i = 0; i < days; i++) {
            const currentDate = new Date(startDate);
            currentDate.setDate(startDate.getDate() + i);
            
            const dayData = itineraryData[i] || {
                diaNumero: i + 1,
                titulo: `Día ${i + 1}`,
                descripcion: '',
                puntoPartida: '',
                puntoLlegada: '',
                duracionEstimada: ''
            };
            
            const dayElement = document.createElement('div');
            dayElement.className = 'accordion-item';
            dayElement.innerHTML = `
                <h2 class="accordion-header">
                    <button class="accordion-button ${i === 0 ? '' : 'collapsed'}" type="button" data-bs-toggle="collapse" data-bs-target="#day${i + 1}">
                        <div class="d-flex align-items-center w-100">
                            <span class="badge bg-primary me-2">Día ${i + 1}</span>
                            <span class="flex-grow-1">${currentDate.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' })}</span>
                        </div>
                    </button>
                </h2>
                <div id="day${i + 1}" class="accordion-collapse collapse ${i === 0 ? 'show' : ''}">
                    <div class="accordion-body">
                        <div class="mb-3">
                            <label class="form-label">Título del día</label>
                            <input type="text" class="form-control day-title" data-day="${i}" value="${dayData.titulo || ''}" placeholder="Ej: Llegada y recorrido por el centro histórico">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Descripción de actividades</label>
                            <textarea class="form-control day-description" data-day="${i}" rows="3" placeholder="Describe las actividades planificadas para este día">${dayData.descripcion || ''}</textarea>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label">Punto de partida</label>
                                <input type="text" class="form-control day-start" data-day="${i}" value="${dayData.puntoPartida || ''}" placeholder="Ej: Hotel en el centro">
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label">Punto de llegada</label>
                                <input type="text" class="form-control day-end" data-day="${i}" value="${dayData.puntoLlegada || ''}" placeholder="Ej: Mirador del valle">
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Duración estimada</label>
                            <select class="form-select day-duration" data-day="${i}">
                                <option value="">Seleccionar duración</option>
                                <option value="2 horas" ${dayData.duracionEstimada === '2 horas' ? 'selected' : ''}>2 horas</option>
                                <option value="4 horas" ${dayData.duracionEstimada === '4 horas' ? 'selected' : ''}>4 horas</option>
                                <option value="6 horas" ${dayData.duracionEstimada === '6 horas' ? 'selected' : ''}>6 horas</option>
                                <option value="8 horas" ${dayData.duracionEstimada === '8 horas' ? 'selected' : ''}>8 horas</option>
                                <option value="Todo el día" ${dayData.duracionEstimada === 'Todo el día' ? 'selected' : ''}>Todo el día</option>
                            </select>
                        </div>
                    </div>
                </div>
            `;
            
            itineraryAccordion.appendChild(dayElement);
        }
        
        // Agregar event listeners a los campos del itinerario
        addItineraryEventListeners();
        updateItineraryJson();
    }
    
    function addItineraryEventListeners() {
        const titleInputs = document.querySelectorAll('.day-title');
        const descriptionInputs = document.querySelectorAll('.day-description');
        const startInputs = document.querySelectorAll('.day-start');
        const endInputs = document.querySelectorAll('.day-end');
        const durationSelects = document.querySelectorAll('.day-duration');
        
        [...titleInputs, ...descriptionInputs, ...startInputs, ...endInputs, ...durationSelects].forEach(input => {
            input.addEventListener('input', updateItineraryData);
            input.addEventListener('change', updateItineraryData);
        });
    }
    
    function updateItineraryData(e) {
        const dayIndex = parseInt(e.target.getAttribute('data-day'));
        const field = e.target.className.split(' ')[1].replace('day-', '');
        
        let fieldName;
        switch(field) {
            case 'title': fieldName = 'titulo'; break;
            case 'description': fieldName = 'descripcion'; break;
            case 'start': fieldName = 'puntoPartida'; break;
            case 'end': fieldName = 'puntoLlegada'; break;
            case 'duration': fieldName = 'duracionEstimada'; break;
        }
        
        if (itineraryData[dayIndex] && fieldName) {
            itineraryData[dayIndex][fieldName] = e.target.value;
            updateItineraryJson();
        }
    }
    
    function updateItineraryJson() {
        if (diasItinerarioJson) {
            diasItinerarioJson.value = JSON.stringify(itineraryData);
        }
    }
    
    // Manejo del rango de edad
    const rangoEdadMin = document.getElementById('rangoEdadMin');
    const rangoEdadMax = document.getElementById('rangoEdadMax');
    const edadMinDisplay = document.getElementById('edad-min-display');
    const edadMaxDisplay = document.getElementById('edad-max-display');
    
    if (rangoEdadMin && rangoEdadMax && edadMinDisplay && edadMaxDisplay) {
        rangoEdadMin.addEventListener('input', function() {
            edadMinDisplay.textContent = this.value;
            if (parseInt(this.value) > parseInt(rangoEdadMax.value)) {
                showNotification('warning', 'Rango de edad inválido', 'La edad mínima no puede ser mayor a la edad máxima');
            }
            if (!isEditMode) {
                validateInfoTabRealTime();
            }
        });
        
        rangoEdadMax.addEventListener('input', function() {
            edadMaxDisplay.textContent = this.value;
            if (parseInt(this.value) < parseInt(rangoEdadMin.value)) {
                showNotification('warning', 'Rango de edad inválido', 'La edad máxima no puede ser menor a la edad mínima');
            }
            if (!isEditMode) {
                validateInfoTabRealTime();
            }
        });
        
        // Inicializar valores
        edadMinDisplay.textContent = rangoEdadMin.value || '18';
        edadMaxDisplay.textContent = rangoEdadMax.value || '60';
    }
    
    // Validación del formulario
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            
            // Validar todos los tabs (solo en modo creación)
            if (!isEditMode) {
                if (!validateInfoTab()) {
                    new bootstrap.Tab(infoTab).show();
                    return;
                }
                
                if (!validateLocationTab()) {
                    new bootstrap.Tab(locationTab).show();
                    return;
                }
            }

            // Confirmar envío con SweetAlert
            const title = isEditMode ? '¿Actualizar grupo de viaje?' : '¿Crear grupo de viaje?';
            const text = isEditMode ? 'Se actualizará el grupo con la información proporcionada' : 'Se creará el grupo con la información proporcionada';
            const confirmText = isEditMode ? 'Sí, actualizar grupo' : 'Sí, crear grupo';
            const loadingTitle = isEditMode ? 'Actualizando grupo...' : 'Creando grupo...';
            const loadingText = isEditMode ? 'Por favor espera mientras actualizamos tu grupo' : 'Por favor espera mientras procesamos tu solicitud';
            
            Swal.fire({
                title: title,
                text: text,
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#3085d6',
                cancelButtonColor: '#d33',
                confirmButtonText: confirmText,
                cancelButtonText: 'Cancelar'
            }).then((result) => {
                if (result.isConfirmed) {
                    // Mostrar loading
                    Swal.fire({
                        title: loadingTitle,
                        text: loadingText,
                        allowOutsideClick: false,
                        didOpen: () => {
                            Swal.showLoading()
                        }
                    });
                    
                    // Enviar el formulario
                    form.submit();
                }
            });
        });
    }
    
    function validateInfoTab(silent = false) {
        let valid = true;
        const errors = [];

        // Validar nombre del viaje
        const nombreViaje = document.getElementById('nombreViaje');
        if (nombreViaje && (!nombreViaje.value || nombreViaje.value.length < 3)) {
            errors.push('El nombre del viaje debe tener al menos 3 caracteres');
            valid = false;
        }

        // Validar destino
        const destinoPrincipal = document.getElementById('destinoPrincipal');
        if (destinoPrincipal && (!destinoPrincipal.value || destinoPrincipal.value.length < 2)) {
            errors.push('El destino principal debe tener al menos 2 caracteres');
            valid = false;
        }

        // Validar fechas
        const fechaInicioInput = document.getElementById('fechaInicio');
        const fechaFinInput = document.getElementById('fechaFin');
        
        if (fechaInicioInput && fechaFinInput) {
            if (!fechaInicioInput.value || !fechaFinInput.value) {
                errors.push('Las fechas de inicio y fin son obligatorias');
                valid = false;
            } else {
                const startDate = new Date(fechaInicioInput.value);
                const endDate = new Date(fechaFinInput.value);
                const today = new Date();
                today.setHours(0, 0, 0, 0);

                if (startDate < today) {
                    errors.push('La fecha de inicio debe ser en el futuro');
                    valid = false;
                }

                if (endDate < startDate) {
                    errors.push('La fecha de fin debe ser posterior a la fecha de inicio');
                    valid = false;
                }
            }
        }

        // Validar descripción
        const descripcion = document.getElementById('descripcion');
        if (descripcion && (!descripcion.value || descripcion.value.length < 10)) {
            errors.push('La descripción debe tener al menos 10 caracteres');
            valid = false;
        }

        // Validar participantes
        const maxParticipantes = document.getElementById('participantes');
        if (maxParticipantes && !maxParticipantes.value) {
            errors.push('Debe seleccionar el número máximo de participantes');
            valid = false;
        }

        // Validar etiquetas
        if (tags.length === 0) {
            errors.push('Debe agregar al menos una etiqueta');
            valid = false;
        }

        // Validar rango de edad
        if (rangoEdadMin && rangoEdadMax) {
            const rangoMin = parseInt(rangoEdadMin.value);
            const rangoMax = parseInt(rangoEdadMax.value);
            
            if (rangoMin > rangoMax) {
                errors.push('La edad máxima debe ser mayor o igual a la edad mínima');
                valid = false;
            }
        }

        // Validar URL de imagen si se proporciona
        const imagenDestacada = document.getElementById('imagenDestacada');
        if (imagenDestacada && imagenDestacada.value && !imagenDestacada.value.match(/^https?:\/\/.+/)) {
            errors.push('La URL de la imagen debe comenzar con http:// o https://');
            valid = false;
        }

        if (!valid && !silent) {
            showNotification('error', 'Errores en el formulario', errors.join('\n'));
        }

        return valid;
    }
    
    function validateLocationTab(silent = false) {
        const puntoEncuentro = document.getElementById('puntoEncuentro');
        if (puntoEncuentro && (!puntoEncuentro.value || puntoEncuentro.value.length < 5)) {
            if (!silent) {
                showNotification('error', 'Punto de encuentro inválido', 'El punto de encuentro debe tener al menos 5 caracteres');
            }
            return false;
        }
        return true;
    }
    
    // Escuchar cambios en las fechas para actualizar el itinerario
    if (fechaInicio) {
        fechaInicio.addEventListener('change', function() {
            if (itineraryTab && document.getElementById('itinerary').classList.contains('show')) {
                updateItinerary();
            }
        });
    }
    
    if (fechaFin) {
        fechaFin.addEventListener('change', function() {
            if (itineraryTab && document.getElementById('itinerary').classList.contains('show')) {
                updateItinerary();
            }
        });
    }

    // Configurar SweetAlert en español
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer)
            toast.addEventListener('mouseleave', Swal.resumeTimer)
        }
    });

    // Función para mostrar notificaciones
    function showNotification(type, title, text) {
        if (type === 'toast') {
            Toast.fire({
                icon: 'success',
                title: title
            });
        } else {
            Swal.fire({
                icon: type,
                title: title,
                text: text,
                confirmButtonText: 'Entendido'
            });
        }
    }
});
