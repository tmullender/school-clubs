const csvFilePath = document.getElementById('csv-file-path');
const dataNode = document.getElementById('data');

document.getElementById('csv-file-select').addEventListener('click', () => {
    window.selectFile(['openFile'], csvFilePath);
}, false);

document.getElementById('start').addEventListener('click', () => {
    if (!csvFilePath.value || /^\s*$/.test(csvFilePath.value)) {
        document.getElementById('setupErrorMessage').textContent = "You need to select a CSV file";
    } else {
        document.getElementById('allocatingModal').style.display = "block";
        document.getElementById('setupModal').style.display = "none";
        window.startAllocation(csvFilePath.value);
    }
}, false);

document.getElementById('allocate').addEventListener('click', () => {
    document.getElementById('configurationModal').style.display = "none";
    document.getElementById('allocatingModal').style.display = "block";
    let pupils = JSON.parse(dataNode.dataset.pupils);
    let clubs = collectClubData();
    dataNode.dataset.clubs = JSON.stringify(clubs);
    window.allocate({ pupils: pupils, clubs: clubs });
});

Array.prototype.forEach.call(document.getElementsByClassName('tablinks'), element => {
    element.addEventListener('click', event => {
        window.switchTab(element.dataset.tabId);
    });
});

document.getElementById('save').addEventListener('click', () => {
    let clubs = collectClubData();
    window.saveClubs(clubs);
});

document.getElementById('search').addEventListener('keyup', (event) => {
    let pupils = document.getElementById('pupilList').getElementsByTagName('button');
    Array.prototype.forEach.call(pupils, pupil => {
        if (pupil.dataset.id.toLowerCase().indexOf(event.srcElement.value.toLowerCase()) > -1) {
            pupil.style.display = "";
        } else {
            pupil.style.display = "none";
        }
    })
});

document.getElementById('addClub').addEventListener('click', (event) => {
    let pupils = JSON.parse(dataNode.dataset.pupils);
    let clubs = JSON.parse(dataNode.dataset.clubs);
    let pupil = event.srcElement.dataset.id;
    let club = document.getElementById('availableClubs').value;
    console.log('Adding ' + pupil + ' to ' + club);
    clubs[club].allocated.push(pupil);
    pupils[pupil].allocated.push(club);
    dataNode.dataset.clubs = JSON.stringify(clubs);
    dataNode.dataset.pupils = JSON.stringify(pupils);
    window.selectPupil(event);
});

function collectClubData() {
    let clubs = JSON.parse(dataNode.dataset.clubs);
    let configurations = document.getElementById('clubConfigurations');
    Array.prototype.forEach.call(configurations.getElementsByTagName('div'), element => {
        let value = element.getElementsByTagName('select')[0].value;
        let key = element.dataset.id;
        clubs[key].maximum = value;
    });
    return clubs;
}

document.getElementById('setupModal').style.display = "block";