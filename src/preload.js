const { dialog } = require('electron').remote;
const ipc = require('electron').ipcRenderer;
const log = require('electron-log');

log.info('Running preload');

window.selectFile = (properties, element) => {
    dialog.showOpenDialog({ properties: properties }, function (fileNames) {
        if(fileNames === undefined || fileNames[0] === undefined) {
            console.log("No file selected");
        } else {
            element.value = fileNames[0];
        }
    });
}

window.startAllocation = (input) => {
    ipc.send('start-allocation', input);
}

window.allocate = (input) => {
    ipc.send('allocate', input);
}

window.saveClubs = (clubs) => {
    ipc.send('save-clubs', clubs);
}

window.switchTab = (tabName) => {
    // Declare all variables
    var i, tabcontent, tablinks;

    // Get all elements with class="tabcontent" and hide them
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    // Get all elements with class="tablinks" and remove the class "active"
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
        if (tablinks[i].dataset.tabId == tabName) {
            tablinks[i].className += " active"
        }
    }

    // Show the current tab, and add an "active" class to the button that opened the tab
    let tabElement = document.getElementById(tabName);
    if (!tabElement.dataset.selected) {
        tabElement.getElementsByClassName('selectable')[0].click();
    }
    tabElement.style.display = "block";
}

ipc.on('read-complete', (event, data) => {
    let configs = document.getElementById('clubConfigurations');
    for (let club of Object.values(data.clubs)) {
        let div = document.createElement('div');
        div.setAttribute('data-id', club.key);
        div.setAttribute('class', 'configuration');
        let span = document.createElement('span');
        span.innerHTML = club.description;
        let select = document.createElement('select');
        for (let i=1; i<=30; i++) {
            let option = document.createElement('option');
            option.setAttribute("value", i);
            option.setAttribute("class", "count");
            if (i == club.maximum) {
                option.setAttribute("selected", "selected");
            }
            option.innerHTML = i;
            select.appendChild(option);
        }
        div.appendChild(span);
        div.appendChild(select);
        configs.appendChild(div);
    }
    dataNode.dataset.classes = JSON.stringify(data.classes);
    dataNode.dataset.clubs = JSON.stringify(data.clubs);
    dataNode.dataset.pupils = JSON.stringify(data.pupils);
    document.getElementById('configurationModal').style.display = "block";
    document.getElementById('allocatingModal').style.display = "none";
});

ipc.on('allocation-complete', (event, data) => {
    console.log(data);
    let dataNode = document.getElementById('data');
    dataNode.dataset.clubs = JSON.stringify(data.clubs);
    dataNode.dataset.pupils = JSON.stringify(data.pupils);
    reloadData();
    switchTab('Pupils');
    document.getElementById('allocatingModal').style.display = "none";
}); 

function reloadData() {
    let data = document.getElementById('data');
    let clubList = document.getElementById('clubList');
    clubList.innerHTML = '';
    for (let club of Object.values(JSON.parse(data.dataset.clubs))) {
        let button = document.createElement('button');
        button.setAttribute('class', 'selectable');
        button.setAttribute('data-id', club.key);
        button.innerHTML = club.description;
        button.addEventListener('click', selectClub);
        clubList.appendChild(button);
    }
    let classList = document.getElementById('classList');
    classList.innerHTML = '';
    for (let clazz of Object.keys(JSON.parse(data.dataset.classes))) {
        let button = document.createElement('button');
        button.setAttribute('class', 'selectable');
        button.setAttribute('data-id', clazz);
        button.innerHTML = clazz;
        button.addEventListener('click', selectClass);
        classList.appendChild(button);
    }
    let pupilList = document.getElementById('pupilList');
    pupilList.innerHTML = '';
    for (let pupil of Object.keys(JSON.parse(data.dataset.pupils))) {
        let button = document.createElement('button');
        button.setAttribute('class', 'selectable');
        button.setAttribute('data-id', pupil);
        button.innerHTML = pupil;
        button.addEventListener('click', selectPupil);
        pupilList.appendChild(button);
    }
}

function selectClub(event) {
    let clubs = JSON.parse(dataNode.dataset.clubs);
    let pupils = JSON.parse(dataNode.dataset.pupils);
    let club = clubs[event.srcElement.dataset.id];
    let temp = document.getElementById("selectedClubList");
    let list = temp.cloneNode(false)
    temp.parentNode.replaceChild(list, temp);
    for (let pupil of club.allocated) {
        let button = document.createElement('button');
        button.setAttribute('class', 'selectedListItem');
        button.setAttribute('data-id', pupil);
        button.innerHTML = pupil + ' (' + pupils[pupil].class + ')';
        button.addEventListener('click', selectPupil)
        list.appendChild(button);
    }
    document.getElementById('clubCount').innerHTML = club.allocated.length;
    let buttons = document.getElementById('clubList').getElementsByTagName('button')
    Array.prototype.forEach.call(buttons, element => {
        element.className = element.className.replace(" active", "");
    });
    document.getElementById('Pupils').dataset.selected = club.key;
    event.currentTarget.className += " active";
}

function selectClass(event) {
    let classes = JSON.parse(dataNode.dataset.classes);
    let clazz = classes[event.srcElement.dataset.id];
    let temp = document.getElementById("selectedClassList");
    let list = temp.cloneNode(false)
    temp.parentNode.replaceChild(list, temp);
    for (let pupil of clazz) {
        let button = document.createElement('button');
        button.setAttribute('class', 'selectedListItem');
        button.setAttribute('data-id', pupil);
        button.innerHTML = pupil;
        button.addEventListener('click', selectPupil)
        list.appendChild(button);
    }
    document.getElementById('classCount').innerHTML = clazz.length;
    let buttons = document.getElementById('classList').getElementsByTagName('button')
    Array.prototype.forEach.call(buttons, element => {
        element.className = element.className.replace(" active", "");
    });
    document.getElementById('Classes').dataset.selected = clazz;
    event.currentTarget.className += " active";
}

function selectPupil(event) {
    let pupils = JSON.parse(dataNode.dataset.pupils);
    let clubs = JSON.parse(dataNode.dataset.clubs);
    let pupil = pupils[event.srcElement.dataset.id];
    document.getElementById('selectedClass').innerHTML = pupil.class;
    document.getElementById('selectedTime').innerHTML = pupil.timestamp;
    let temp = document.getElementById('selectedRequests');
    let requests = temp.cloneNode(false);
    temp.parentNode.replaceChild(requests, temp);
    pupil.requests.filter(r => r != null).forEach(request => {
        let div = document.createElement('div');
        div.innerHTML = clubs[request].description;
        requests.appendChild(div);
    });
    temp = document.getElementById('selectedAllocations');
    requests = temp.cloneNode(false);
    temp.parentNode.replaceChild(requests, temp);
    for (let request of pupil.allocated) {
        let div = document.createElement('div');
        div.innerHTML = clubs[request].description;
        requests.appendChild(div);
    }
    let buttons = document.getElementById('pupilList').getElementsByTagName('button')
    Array.prototype.forEach.call(buttons, element => {
        element.className = element.className.replace(" active", "");
        if (element.dataset.id == pupil.name) {
            element.className += " active";
        } 
    });
    document.getElementById('Pupils').dataset.selected = pupil.name;
    window.switchTab('Pupils');
}