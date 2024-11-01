# Creator Dao

## DAO 정보
* id(Utf8): DAO ID
* name(Utf8): 이름
* description(Utf8): 설명

## DAO 참여자
* Founder 창립자
  * 크리에이터 본인
  * DAO에 관한 모든 권한
* Coordinator 시스템 관리자
  * playNomm 계정으로 세팅
  * DAO에 관한 모든 권한
* Moderator 일반 관리자
  * 해산 제외한 나머지 관리권한
* Member 참여자
  * 표결 참여
* Applicant
  * 가입 신청한 사람
* 그 외
  * 가입신청

## 액션
* Coordinator만 가능
  * ReplaceCoordinator
* Founder, Coordinator만 가능
  * DAO 개설
  * 관리자 임명, 해임, DAO 해산
* Moderator 이상 가능
  * DAO 정보변경
  * 회원 가입, 탈퇴, 안건 발의
* Member 이상 가능
  * 표결 참여
* 그 외
  * Dao 가입신청

## 트랜잭션

### CreateCreatorDao (개설)
Founder나 Coordinator가 새로운 DAO를 만든다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "founder",
      "sig": {
        "v": 27,
        "r": "62d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "2d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "CreateCreatorDao": {
        "networkId": 102,
        "createdAt": "2024-03-15T09:28:41.339Z",
        "id": "dao_001",
        "name": "Art Creators DAO",
        "description": "A DAO for digital art creators",
        "founder": "creator001",
        "coordinator": "playnomm"
      }
    }
  }
}
```

### UpdateCreatorDao (정보변경)
Moderator 이상 권한을 가진 사용자가 DAO 정보를 수정한다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "moderator",
      "sig": {
        "v": 27,
        "r": "72d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "3d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "UpdateCreatorDao": {
        "networkId": 102,
        "createdAt": "2024-03-15T10:28:41.339Z",
        "id": "dao_001",
        "name": "Digital Art Creators DAO",
        "description": "A DAO for digital art creators and collectors"
      }
    }
  }
}
```

### DisbandCreatorDao (해산)
Founder나 Coordinator만 DAO를 해산할 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "founder",
      "sig": {
        "v": 27,
        "r": "82d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "4d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "DisbandCreatorDao": {
        "networkId": 102,
        "createdAt": "2024-03-15T11:28:41.339Z",
        "id": "dao_001"
      }
    }
  }
}
```

### ReplaceCoordinator (코디네이터 변경)
현재 Coordinator만 새로운 Coordinator를 지정할 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "coordinator",
      "sig": {
        "v": 27,
        "r": "92d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "5d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "ReplaceCoordinator": {
        "networkId": 102,
        "createdAt": "2024-03-15T12:28:41.339Z",
        "id": "dao_001",
        "newCoordinator": "playnomm2"
      }
    }
  }
}
```

### AddMembers (참여자 추가)
Moderator 이상 권한을 가진 사용자가 새로운 멤버를 추가할 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "moderator",
      "sig": {
        "v": 27,
        "r": "a2d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "6d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "AddMembers": {
        "networkId": 102,
        "createdAt": "2024-03-15T13:28:41.339Z",
        "id": "dao_001",
        "members": ["user001", "user002", "user003"]
      }
    }
  }
}
```

### RemoveMembers (참여자 제외)
Moderator 이상 권한을 가진 사용자가 멤버를 제외할 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "moderator",
      "sig": {
        "v": 27,
        "r": "b2d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "7d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "RemoveMembers": {
        "networkId": 102,
        "createdAt": "2024-03-15T14:28:41.339Z",
        "id": "dao_001",
        "members": ["user003"]
      }
    }
  }
}
```

### PromoteModerators (관리자 임명)
Founder나 Coordinator가 일반 멤버를 Moderator로 승급시킬 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "founder",
      "sig": {
        "v": 27,
        "r": "c2d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "8d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "PromoteModerators": {
        "networkId": 102,
        "createdAt": "2024-03-15T15:28:41.339Z",
        "id": "dao_001",
        "members": ["user001"]
      }
    }
  }
}
```

### DemoteModerators (관리자 해임)
Founder나 Coordinator가 Moderator를 일반 멤버로 강등시킬 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "founder",
      "sig": {
        "v": 27,
        "r": "d2d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "9d546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "DemoteModerators": {
        "networkId": 102,
        "createdAt": "2024-03-15T16:28:41.339Z",
        "id": "dao_001",
        "members": ["user001"]
      }
    }
  }
}
```

### ApplyCreatorDao (가입신청)
DAO에 가입하고 싶은 사용자가 신청할 수 있다.

```json
{
  "sig": {
    "NamedSignature": {
      "name": "applicant",
      "sig": {
        "v": 27,
        "r": "e2d7c7ddf8bea783b8ed59906b2f5db00b9e53031d6407933d7c4a80c7157f35",
        "s": "ad546c7d0f0fdf058e5bdf74b39cb2d3db34aa1dcdd6b2a76ea6504655b12b0f"
      }
    }
  },
  "value": {
    "CreatorDaoTx": {
      "ApplyCreatorDao": {
        "networkId": 102,
        "createdAt": "2024-03-15T17:28:41.339Z",
        "id": "dao_001",
        "account": "user004"
      }
    }
  }
}
```

## 공통 사항
- 모든 트랜잭션에는 networkId와 createdAt을 포함한다.
- 서명은 NamedSignature 형식을 사용하고, 권한에 맞는 이름을 포함한다.
- 멤버 관련 작업(추가/제거/승급/강등)은 배열로 여러 계정을 한번에 처리할 수 있다.
